package com.ticket.fast.ticket.service;

import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.ticket.domain.*;
import com.ticket.fast.ticket.dto.event.PaymentEvent;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.dto.SeatInfo;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final PerformanceEventHub eventHub;
    private final PerformanceRepository performanceRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final TransactionalOperator transactionalOperator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;


    @Value("${reservation-expire-minute}")
    private int RESERVATION_EXPIRE_MINUTE;

    // db만 사용한 저장
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
                                    .doOnSuccess(saved -> eventHub.publish(new SeatStatusEvent(
                                            saved.getPerformanceId(),
                                            saved.getSeatCode(),
                                            SeatStatus.RESERVED,
                                            LocalDateTime.now()
                                    )));
                        }))
                .doOnNext(reservation -> log.info("예약 저장 성공 id: {}", reservation.getId()))
                .map(ReservationResponse::fromEntity);
    }

    // db + redis 를 사용해서 예약
    public Mono<ReservationResponse> createReservationByRedis(AuthUser authUser, ReservationCreateRequest request){
        String key = TicketUtil.createPerformanceRedisKey(request.performanceId());
        return redisTemplate.opsForSet().remove(key, String.valueOf(request.performanceSeatId()))
                .flatMap(removedCount -> {
                    // 좌석 선점 성공
                    if (removedCount == 1) {
                        return saveReservationToDb(authUser, request).as(transactionalOperator::transactional)
                                .doOnSuccess(saved -> eventHub.publish(new SeatStatusEvent(
                                        saved.performanceId(),
                                        saved.seatCode(),
                                        SeatStatus.RESERVED,
                                        LocalDateTime.now()
                                )));
                    }

                    return Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION));
                }).onErrorResume(
                        e -> {
                            if (e instanceof BusinessException) return Mono.error(e);

                            log.error("예약중 에러 발생 e {}",e.getMessage(),e);
                            return redisTemplate.opsForSet().add(key, String.valueOf(request.performanceSeatId()))
                                    .then(Mono.error(new BusinessException(ErrorCode.RESERVATION_NOT_SAVED)));
                        }
                );
    }

    // redis + kafka 를 사용해서 저장
    public Mono<ReservationResponse> createReservationByRedisAndKafka(AuthUser authUser, ReservationCreateRequest request){
        String seatSetKey = TicketUtil.createPerformanceRedisKey(request.performanceId());
        String seatDetailKey = TicketUtil.createDetailKey(seatSetKey);
        String seatId = String.valueOf(request.performanceSeatId());

        return redisTemplate.opsForSet().remove(seatSetKey, seatId)
                .flatMap(removedCount -> {
                    //좌석 선점 완료 경우
                    if (removedCount == 1) {
                        return redisTemplate.opsForHash().get(seatDetailKey, seatId)
                                .map(json -> objectMapper.readValue(json.toString(), SeatInfo.class))
                                .flatMap(info -> {
                                    ReservationEvent event = new ReservationEvent(
                                            authUser.userId(),
                                            request.performanceId(),
                                            request.performanceSeatId(),
                                            info.seatCode(),
                                            info.price()
                                    );

                                    return Mono.fromFuture(kafkaTemplate.send("ticketing-topic", event))
                                            .doOnError(e -> log.error(" [CRITICAL] kafka 전송 최종 실패 유저 d: {}, 좌석id: {}, 에러: {}",
                                                    authUser.userId(),seatId,e.getMessage(),e))
                                            .thenReturn(ReservationResponse.pending(authUser.userId(), request, info));
                                });

                    }

                    return Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION));
                }).onErrorResume(
                        e -> {
                            // 단순 좌석이 이미 선점되어서 발생한 오류인 경우
                            if (e instanceof BusinessException) return Mono.error(e);

                            log.error("예약중 에러 발생 e {}",e.getMessage(),e);
                            // 좌석 선점이 아닌 다른 예상치 못한 오류의 경우 -> redis 에 좌석 복구
                            return redisTemplate.opsForSet().add(seatSetKey, String.valueOf(request.performanceSeatId()))
                                    .then(Mono.error(new BusinessException(ErrorCode.RESERVATION_NOT_SAVED)));
                        }
                );
    }



    private Mono<ReservationResponse> saveReservationToDb(AuthUser authUser, ReservationCreateRequest request){
        return performanceSeatRepository.findById(request.performanceSeatId())
                .flatMap(seat -> performanceSeatRepository.reserveSeat(seat.getId()).flatMap(updatedRows -> {
                            if (updatedRows == 0) {
                                return Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION));
                            }
                            return reservationRepository.save(
                                    Reservation.builder()
                                            .performanceId(seat.getPerformanceId())
                                            .userId(authUser.userId())
                                            .price(seat.getPrice())
                                            .seatCode(seat.getSeatCode())
                                            .status(ReservationStatus.PENDING)
                                            .build());
                        }
                ).map(ReservationResponse::fromEntity));
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
                            .userId(reservation.getUserId())
                            .amount(req.amount())
                            .reservationId(reservation.getId())
                            .method(req.method())
                            .status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAIL)
                            .build();
                    return paymentHistoryRepository.save(paymentHistory).map(PaymentResponse::fromEntity);
                });
    }

    public Mono<Void> approvePaymentByKafka(AuthUser authUser, PaymentRequest request){
        return reservationRepository.findById(request.reservationId())
                .filter(res -> res.getStatus().equals(ReservationStatus.PENDING))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_AVAILABLE_RESERVATION)))
                .flatMap(reservation -> {
                    PaymentEvent paymentEvent = new PaymentEvent(
                            authUser.userId(),
                            reservation.getId(),
                            request.amount(),
                            request.method()
                    );
                    return Mono.fromFuture(kafkaTemplate.send("payment-topic", paymentEvent))
                            .then();
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

    @Transactional
    public Mono<ReservationResponse> cancelReservation(AuthUser authUser, Long reservationId){

        return reservationRepository.findById(reservationId)
                //권한 체크
                .filter(r -> r.getUserId().equals(authUser.userId()))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.NOT_RESERVATION_OWNER)))

                //상태 체크
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.ALREADY_CANCELLED_RESERVATION)))

                //원자적 취소 실행
                .flatMap(r -> reservationRepository.cancelReservation(reservationId))

                //성공 여부 확인 후 데이터 재조회
                .flatMap(count -> {
                    if (count == 0) {
                        // 업데이트가 안 됐다면? 그 사이에 10분이 지났거나 상태가 변한 것
                        return Mono.error(new BusinessException(ErrorCode.CANCELLATION_FAILED));
                    }
                    return reservationRepository.findById(reservationId);
                }).map(ReservationResponse::fromEntity)
                .doOnSuccess(r -> log.info("예약 취소 성공 예약 id{}", reservationId))
                .doOnError(e -> log.error("예약 취소중 오류 발생 예약 id {}",reservationId));
    }

    /**
     * 결제 대기후 일정 시간동안 확정되지 않은(미결제) 예약을 만료 시키고 좌석 원복
     * @return
     */
    public Mono<Void> processExpiredReservations() {

        LocalDateTime expirationThreshold =
                LocalDateTime.now()
                        .minusMinutes(RESERVATION_EXPIRE_MINUTE);

        return reservationRepository
                .findTop1000ByStatusAndCreatedAtBefore(
                        ReservationStatus.PENDING,
                        expirationThreshold
                )
                .concatMap(reservation ->

                        expireReservationInDb(reservation)
                                .as(transactionalOperator::transactional)

                                // DB 트랜잭션 종료 이후
                                .flatMap(seat ->
                                        restoreRedis(seat)
                                                .then(publishSeatReleaseEvent(reservation))
                                                .thenReturn(seat)
                                )

                                .doOnSuccess(seat ->
                                        log.info(
                                                "만료 처리 완료 예약 id {}, 유저 id {}, 공연 id {}",
                                                reservation.getId(),
                                                reservation.getUserId(),
                                                reservation.getPerformanceId()
                                        )
                                )

                                .onErrorResume(ex -> {
                                    log.error(
                                            "예약 만료 처리 실패 reservationId={}",
                                            reservation.getId(),
                                            ex
                                    );
                                    return Mono.empty();
                                })
                )
                .then();
    }

    private Mono<Void> restoreRedis(PerformanceSeat seat) {

        String seatSetKey =
                TicketUtil.createPerformanceRedisKey(seat.getPerformanceId());

        String seatDetailKey =
                TicketUtil.createDetailKey(seatSetKey);

        String seatId = String.valueOf(seat.getId());

        SeatInfo info = new SeatInfo(seat.getSeatCode(), seat.getPrice());

        return Mono.fromCallable(() ->
                        objectMapper.writeValueAsString(info)
                )
                .flatMap(json ->
                        Mono.zip(
                                redisTemplate.opsForSet()
                                        .add(seatSetKey, seatId),

                                redisTemplate.opsForHash()
                                        .put(seatDetailKey, seatId, json)
                        )
                )
                .then();
    }

    private Mono<PerformanceSeat> expireReservationInDb(Reservation reservation) {

        return performanceSeatRepository.findByPerformanceIdAndSeatCode(
                        reservation.getPerformanceId(),
                        reservation.getSeatCode()
                )
                .switchIfEmpty(Mono.error(
                        new BusinessException(ErrorCode.SEAT_NOT_FOUND)
                ))
                .flatMap(seat ->
                        performanceSeatRepository.releaseSeat(
                                        reservation.getPerformanceId(),
                                        reservation.getSeatCode()
                                )
                                .flatMap(updatedRows -> {

                                    if (updatedRows == 0) {
                                        return Mono.error(
                                                new BusinessException(
                                                        ErrorCode.ALREADY_CANCELLED_RESERVATION
                                                )
                                        );
                                    }
                                    seat.release();
                                    return reservationRepository
                                            .delete(reservation)
                                            .thenReturn(seat);
                                })
                );
    }

    private Mono<Void> publishSeatReleaseEvent(
            Reservation reservation
    ) {
        return Mono.fromRunnable(() ->
                eventHub.publish(
                        new SeatStatusEvent(
                                reservation.getPerformanceId(),
                                reservation.getSeatCode(),
                                SeatStatus.AVAILABLE,
                                LocalDateTime.now()
                        )
                )
        );
    }
}
