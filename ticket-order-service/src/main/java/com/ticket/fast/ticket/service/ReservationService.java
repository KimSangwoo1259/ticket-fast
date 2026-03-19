package com.ticket.fast.ticket.service;

import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;
import com.ticket.fast.ticket.dto.response.ReservationResponse;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;


    @Transactional
    public Mono<ReservationResponse> createReservation(AuthUser authUser, ReservationCreateRequest request) {
        return performanceSeatRepository.findByPerformanceIdAndSeatCode(request.performanceId(), request.seatCode())
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.SEAT_NOT_FOUND)))
                .flatMap(seat -> performanceSeatRepository.reserveSeat(seat.getId()))
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
                            .status(ReservationStatus.CONFIRMED)
                            .build());
                })
                // TODO: [TIMEOUT] 좌석 선점(RESERVED) 후 일정 시간 내 결제 미완료 시 자동 취소 스케줄러 연동 필요
                .doOnNext(reservation -> log.info("예약 저장 성공 id: {}", reservation.getId()))
                .map(ReservationResponse::fromEntity);
    }

    public Mono<Page<ReservationResponse>> getMyReservations(AuthUser authUser, Pageable pageable){
        return Mono.zip(
                reservationRepository.findByUserId(authUser.userId(), pageable).map(ReservationResponse::fromEntity)
                        .collectList(),
                reservationRepository.countByUserId(authUser.userId()))
                .doOnSuccess(responses -> log.info("개인 저장 내역 불러오기 성공 userId {}",authUser.userId()))
                .doOnError(e -> log.error("예약 내역 조회중 오류 발생 userId {}, error {}",authUser.userId(),e.getMessage(),e))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }
}
