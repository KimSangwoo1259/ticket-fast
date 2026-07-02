package com.ticket.fast.ticket.consumer;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationConsumer {
    private final ReservationRepository reservationRepository;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final TransactionalOperator transactionalOperator;


    @KafkaListener(
            topics = "ticketing-topic",
            groupId = "ticket-group-test",
            concurrency = "3",
            properties = {"max.poll.records=50"},
            containerFactory = "batchKafkaListenerContainerFactory")

    public void consume(List<ReservationEvent> events){
        try{
            reservationRepository.saveAllEventsWithIgnore(events)
                    .then(performanceSeatRepository.reserveSeatBulk(events.stream().map(ReservationEvent::performanceSeatId).toList()))
                    .as(transactionalOperator::transactional)
                    .doOnError(e -> log.error("배치 저장중 에러 발생 {}", e.getMessage(), e))
                    .block();
        } catch (Exception bulkException){
            log.warn("벌크 처리주 에러 발생, 순회하여 실패 데이터 탐색. Error {}",bulkException.getMessage());

            for (int i = 0; i < events.size(); i++){
                ReservationEvent event = events.get(i);
                try {
                    reservationRepository.save(ReservationEvent.toEntity(event))
                            .then(performanceSeatRepository.reserveSeat(event.performanceSeatId()))
                            .as(transactionalOperator::transactional)
                            .block();
                } catch (Exception individualException){
                    log.error("배치중 {}번째 데이터 에러 발생: {}", i, individualException.getMessage(), individualException);

                    throw new BatchListenerFailedException("배치 내 특정 데이터 처리 실패", individualException,i);
                }
            }
        }

    }
}
