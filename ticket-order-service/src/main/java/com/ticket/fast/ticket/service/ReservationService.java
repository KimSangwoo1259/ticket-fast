package com.ticket.fast.ticket.service;

import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.ticket.domain.*;
import com.ticket.fast.ticket.dto.request.PaymentRequest;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;
import com.ticket.fast.ticket.dto.response.PaymentResponse;
import com.ticket.fast.ticket.dto.response.ReservationResponse;
import com.ticket.fast.ticket.dto.response.ReservationWithPerformanceResponse;
import com.ticket.fast.ticket.event.PerformanceEventHub;
import com.ticket.fast.ticket.event.dto.SeatStatusEvent;
import com.ticket.fast.ticket.repository.PaymentHistoryRepository;
import com.ticket.fast.ticket.repository.PerformanceRepository;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
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
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${reservation-expire-minute}")
    private int RESERVATION_EXPIRE_MINUTE;


    @Transactional
    public Mono<ReservationResponse> createReservation(AuthUser authUser, ReservationCreateRequest request) {
        // 좌석 조회
        return performanceSeatRepository.findById(request.performanceSeatId())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SEAT_NOT_FOUND)))
                // 예약(update 쿼리)
                .flatMap(seat -> performanceSeatRepository.reserveSeat(seat.getId())
                        .flatMap(updateCount -> {
                            // update = 0 이라면 이미 선점된 좌석
                            if (updateCount == 0) {
                                return Mono.error(new BusinessException(ErrorCode.SEAT_UNAVAILABLE));
                            }
                            return reservationRepository.save(Reservation.builder()
                                            .userId(authUser.userId())
                                            .performanceId(seat.getPerformanceId())
                                            .seatCode(seat.getSeatCode())
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
                .doOnNext(reservation -> log.info("예약 저장 성공 id: {}", reservation.getId()))
                .map(ReservationResponse::fromEntity);
    }

    @Transactional
    public Mono<ReservationResponse> createReservationByRedis(AuthUser authUser, ReservationCreateRequest request){
        String key = TicketUtil.createPerformanceRedisKey(request.performanceId());
        return redisTemplate.opsForSet().remove(key, request.performanceSeatId())
                .flatMap(removedCount -> {
                    if (removedCount == 1) {
                        return saveReservationToDb(authUser, request);
                    }

                    return Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION));
                }).onErrorResume(
                        e -> {
                            if (e instanceof BusinessException) return Mono.error(e);

                            return redisTemplate.opsForSet().add(key, String.valueOf(request.performanceSeatId()))
                                    .then(Mono.error(new BusinessException(ErrorCode.RESERVATION_NOT_SAVED)));
                        }
                );
    }

    private Mono<ReservationResponse> saveReservationToDb(AuthUser authUser, ReservationCreateRequest request){
        return performanceSeatRepository.findById(request.performanceSeatId())
                .flatMap(seat -> reservationRepository.save(
                        Reservation.builder()
                                .userId(authUser.userId())
                                .price(seat.getPrice())
                                .seatCode(seat.getSeatCode())
                                .status(ReservationStatus.PENDING)
                                .build()
                )).map(ReservationResponse::fromEntity);
    }

    @Transactional
    public Mono<PaymentResponse> approvePayment(AuthUser authUser, PaymentRequest request){
        // 예약 있는거 맞아? 확인-> pending 상태 아니면 다 exception

        return reservationRepository.findById(request.reservationId())
                .filter(res -> res.getStatus().equals(ReservationStatus.PENDING))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION)))
                .flatMap(reservation -> {
                    reservation.confirmReservation();

                    return reservationRepository.save(reservation)
                            .then(savePaymentHistory(reservation, request, true));
                });

    }

    private Mono<PaymentResponse> savePaymentHistory(Reservation reservation, PaymentRequest request, boolean success) {
        return Mono.just(request)
                .flatMap(req -> {
                    PaymentHistory paymentHistory = PaymentHistory.builder()
                            .amount(req.amount())
                            .reservationId(reservation.getId())
                            .method(req.method())
                            .status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAIL)
                            .build();

                    return paymentHistoryRepository.save(paymentHistory).map(PaymentResponse::fromEntity);
                });
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

        return reservationRepository.findTop500ByStatusAndCreatedAtBefore(
                ReservationStatus.PENDING, expirationThreshold
        ).collectList()// 조회가 완전히 끝날 때까지 대기 (데드락 방지)
                .flatMapMany(Flux::fromIterable)
                .flatMap(reservation -> {
                    log.info("만료 대상 발견: {}", reservation.getId());
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
        },10).then();

    }
}
