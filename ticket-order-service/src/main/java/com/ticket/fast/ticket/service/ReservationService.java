package com.ticket.fast.ticket.service;

import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;
import com.ticket.fast.ticket.domain.SeatStatus;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;
import com.ticket.fast.ticket.dto.response.ReservationResponse;
import com.ticket.fast.ticket.dto.response.ReservationWithPerformanceResponse;
import com.ticket.fast.ticket.event.PerformanceEventHub;
import com.ticket.fast.ticket.event.dto.SeatStatusEvent;
import com.ticket.fast.ticket.repository.PerformanceRepository;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final PerformanceEventHub eventHub;
    private final PerformanceRepository performanceRepository;

    @Value("${reservation-expire-minute}")
    private int RESERVATION_EXPIRE_MINUTE;


    @Transactional
    public Mono<ReservationResponse> createReservation(AuthUser authUser, ReservationCreateRequest request) {
        return performanceSeatRepository.findByPerformanceIdAndSeatCode(request.performanceId(), request.seatCode())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SEAT_NOT_FOUND)))
                .flatMap(seat -> performanceSeatRepository.reserveSeat(seat.getId())
                        .flatMap(updateCount -> {
                            if (updateCount == 0) {
                                return Mono.error(new BusinessException(ErrorCode.SEAT_UNAVAILABLE));
                            }

                            // TODO: [PAYMENT] 현재는 결제 로직이 없으므로 바로 CONFIRMED 처리.
                            // TODO: 추후 PENDING 상태로 생성 후 결제 완료 웹훅(Webhook) 시점에 CONFIRMED로 변경하도록 고도화 예정.
                            return reservationRepository.save(Reservation.builder()
                                            .userId(authUser.userId())
                                            .performanceId(request.performanceId())
                                            .seatCode(request.seatCode())
                                            .price(seat.getPrice())
                                            .status(ReservationStatus.PENDING)
                                            .build())
                                    .doOnSuccess(saved -> {
                                        eventHub.publish(new SeatStatusEvent(
                                                saved.getPerformanceId(),
                                                saved.getSeatCode(),
                                                SeatStatus.RESERVED,
                                                LocalDateTime.now()
                                        ));
                                    });
                        }))
                // TODO: [TIMEOUT] 좌석 선점(RESERVED) 후 일정 시간 내 결제 미완료 시 자동 취소 스케줄러 연동 필요
                .doOnNext(reservation -> log.info("예약 저장 성공 id: {}", reservation.getId()))
                .map(ReservationResponse::fromEntity);
    }



    public Mono<Page<ReservationWithPerformanceResponse>> getMyReservations(AuthUser authUser, Pageable pageable){
        return Mono.zip(
                        reservationRepository.findByUserId(authUser.userId(), pageable)
                                .flatMap(reservation -> performanceRepository.findById(reservation.getPerformanceId()
                                ).map(performance -> ReservationWithPerformanceResponse.fromEntity(performance, reservation)))
                                .collectList(),
                        reservationRepository.countByUserId(authUser.userId())
                )
                .doOnSuccess(responses -> log.info("개인 저장 내역 불러오기 성공 userId {}", authUser.userId()))
                .doOnError(e -> log.error("예약 내역 조회중 오류 발생 userId {}, error {}", authUser.userId(), e.getMessage(), e))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    //TODO 3번 db 조회가 비효율 적일 수도 있음. 비효율 적으로 판단 될 시, 실패 했을때만 조회하는 방식으로 변경
    @Transactional
    public Mono<ReservationResponse> cancelReservation(AuthUser authUser, Long reservationId){

        return reservationRepository.findById(reservationId)
                // 1. 권한 체크
                .filter(r -> r.getUserId().equals(authUser.userId()))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_RESERVATION_OWNER)))

                // 2. 상태 체크
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ALREADY_CANCELLED_RESERVATION)))

                // 3. 원자적 취소 실행 (결과는 Mono<Long>)
                .flatMap(r -> reservationRepository.cancelReservation(reservationId))

                // 4. 성공 여부 확인 후 데이터 재조회
                .flatMap(count -> {
                    if (count == 0) {
                        // 업데이트가 안 됐다면? 그 사이에 10분이 지났거나 상태가 변한 것
                        return Mono.error(new BusinessException(ErrorCode.CANCELLATION_FAILED));
                    }
                    // 성공했다면 DB에서 '진짜 최신' 데이터를 다시 가져옵니다.
                    return reservationRepository.findById(reservationId);
                }).map(ReservationResponse::fromEntity)
                .doOnSuccess(r -> log.info("예약 취소 성공 예약 id{}", reservationId))
                .doOnError(e -> log.error("예약 취소중 오류 발생 예약 id {}",reservationId));
    }

    /**
     * 결제 대기후 일정 시간동안 확정되지 않은 예약을 만료 시키고 좌석 원복
     * @return
     */
    @Transactional
    public Mono<Void> processExpiredReservations() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(RESERVATION_EXPIRE_MINUTE);

        return reservationRepository.findAllByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING, expirationThreshold
        ).flatMap(reservation -> {
            reservation.expire();

            return performanceSeatRepository.findByPerformanceIdAndSeatCode(reservation.getPerformanceId(), reservation.getSeatCode())
                    .flatMap(seat -> {
                        seat.release();
                        return performanceSeatRepository.save(seat)
                                .doOnSuccess(s -> {
                                    eventHub.publish(new SeatStatusEvent(
                                            reservation.getPerformanceId(),
                                            reservation.getSeatCode(),
                                            SeatStatus.AVAILABLE,
                                            LocalDateTime.now()
                                    ));

                                });
                    }).then(reservationRepository.save(reservation));
        }).then();

    }
}
