package com.ticket.fast.ticket.consumer;


import com.ticket.fast.ticket.domain.PaymentHistory;
import com.ticket.fast.ticket.domain.PaymentStatus;
import com.ticket.fast.ticket.dto.event.PaymentEvent;
import com.ticket.fast.ticket.event.UserNotificationHub;
import com.ticket.fast.ticket.repository.PaymentHistoryRepository;
import com.ticket.fast.ticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {

    private final UserNotificationHub notificationHub;
    private final PaymentHistoryRepository paymentHistoryRepository;


    @KafkaListener(topics = "payment-topic",groupId = "payment-group")
    public void consume(List<PaymentEvent> events){
        List<PaymentHistory> paymentHistories = events.stream()
                .map(event -> PaymentHistory.builder()
                        .userId(event.userId())
                        .amount(event.amount())
                        .method(event.method())
                        .status(PaymentStatus.SUCCESS)
                        .reservationId(event.reservationId())
                        .build()
                ).toList();

        List<Long> newIds = paymentHistories.stream()
                .map(PaymentHistory::getId).toList();

        paymentHistoryRepository.saveAllEventsWithIgnore(paymentHistories)
                //저장 성공한 데이터만 다시 조회
                .thenMany(paymentHistoryRepository.findByIdIn(newIds))
                // 성공한 건에 대해서만 개별 유저에게 SSE 발송
                .doOnNext(history -> notificationHub.publishToUser(
                        history.getUserId(),
                        "PAYMENT_SUCCESS", // 이벤트 이름표
                        String.valueOf(history.getReservationId()) // 프론트가 영수증 조회를 할 수 있도록 예약 ID 전달
                ))

                .doOnError(e -> log.error("결제 배치 처리 중 DB 에러 발생: {}", e.getMessage(), e))
                .blockLast();


    }

}
