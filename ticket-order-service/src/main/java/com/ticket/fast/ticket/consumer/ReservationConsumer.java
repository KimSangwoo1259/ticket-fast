package com.ticket.fast.ticket.consumer;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;
import com.ticket.fast.ticket.dto.ReservationEvent;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationConsumer {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;


    @KafkaListener(topics = "ticketing-topic", groupId = "ticket-group")
    public void consume(List<ReservationEvent> events){
        log.info("수신된 예약 이벤트 개수: {}",events.size());

        List<Reservation> reservations = events.stream()
                .map(event -> Reservation.builder()
                        .userId(event.userId())
                        .performanceId(event.performanceId())
                        .price(event.price())
                        .seatCode(event.seatCode())
                        .status(ReservationStatus.CONFIRMED)
                        .build())
                .toList();

        // 2. 좌석 상태 변경과 예약 저장을 '비동기'로 처리
        reservationRepository.saveAll(reservations)
                .thenMany(Flux.fromIterable(events))
                .flatMap(event -> performanceSeatRepository.reserveSeat(event.performanceSeatId()))
                .collectList()
                .block(); // 또는 리스너 타입에 따라 적절히 처리
    }
}
