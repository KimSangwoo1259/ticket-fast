package com.ticket.fast.ticket.consumer;

import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationConsumer {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;


    @KafkaListener(
            topics = "ticketing-topic",
            groupId = "ticket-group",
            concurrency = "3",
            properties = {"max.poll.records=50"})

    public void consume(List<ReservationEvent> events){
        log.info("수신된 예약 이벤트 개수: {}",events.size());



        reservationRepository.saveAllEventsWithIgnore(events)
                .then(performanceSeatRepository.reserveSeatBulk(events.stream().map(ReservationEvent::performanceSeatId).toList()))
                .doOnError(e -> log.error("배치 저장중 에러 발생 {}", e.getMessage(), e))
                .block();

    }
}
