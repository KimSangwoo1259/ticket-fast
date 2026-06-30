package com.ticket.fast.ticket.listener;

import com.ticket.fast.ticket.domain.FailedReservation;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.repository.FailedReservationRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TicketDlqListener {


    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final FailedReservationRepository failedReservationRepository;

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    public TicketDlqListener(WebClient.Builder webClientBuilder,
                             ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                             FailedReservationRepository failedReservationRepository) {

        this.webClient = webClientBuilder.build();
        this.redisTemplate = reactiveRedisTemplate;
        this.failedReservationRepository = failedReservationRepository;
    }

    @KafkaListener(topics = "ticketing-topic.DLQ", groupId = "ticket-dlq-group")
    public void consumeDlq(
            @Payload List<ReservationEvent> failedEvents,
            @Header(KafkaHeaders.DLT_EXCEPTION_MESSAGE) List<String> exceptionMessages,
            @Header(KafkaHeaders.DLT_EXCEPTION_FQCN) List<String> exceptionTypes){
        log.error("Error detected Failed {} Data enqueued in DLQ, ",failedEvents.size());

        String slackMessage = String.format("""
                        🚨 *[티켓 시스템 크리티컬 장애 알림]* 🚨
                          • *내용:* 메인 컨슈머가 3회 이상 처리에 실패하여 DLQ로 격리되었습니다.
                          • *격리된 이벤트 개수:* %d건
                          • *장애 의심 좌석 ID:* %s (외 %d건)
                          • *조치 요망:* 즉시 서버 로그를 확인하고 데이터를 수동 복구해 주세요.
                        """,
                failedEvents.size(),
                failedEvents.get(0).performanceSeatId(),
                failedEvents.size() - 1);

        List<FailedReservation> failedReservations = new ArrayList<>();

        for (int i = 0; i < failedEvents.size(); i++) {
            ReservationEvent event = failedEvents.get(i);

            String exactReason = exceptionTypes.get(i) + ": " + exceptionMessages.get(i);

            failedReservations.add(FailedReservation.fromEvent(event, exactReason));
        }

        Flux.fromIterable(failedEvents)
                //실패 한 좌석 redis 복구
                .flatMap(event -> {
                    String seatSetKey = TicketUtil.createPerformanceRedisKey(event.performanceId());
                    return redisTemplate.opsForSet().add(seatSetKey, String.valueOf(event.performanceSeatId()));
                })
                //실패 내역 db 저장
                .then(
                        failedReservationRepository.saveAll(failedReservations)
                                .then()
                                .onErrorResume(
                                        e -> {
                                            log.error("실패 이력 DB 저장 실패 {}",e.getMessage(), e);
                                            return Mono.empty();
                                        }
                                )
                )
                // 실패 내역 slack 알림 전송

                .then(
                        webClient.post()
                                .uri(slackWebhookUrl)
                                .bodyValue(Map.of("text", slackMessage))
                                .retrieve()
                                .toBodilessEntity()
                )
                .doOnSuccess(response -> log.info("Redis 롤백(보상 트랜잭션) 및 슬랙 알림 전송 완료"))
                .doOnError(e -> log.error("DLQ 후속 처리(보상/알림) 중 크리티컬 에러 발생: {}", e.getMessage(), e))
                .block();


    }
}
