package com.ticket.fast.ticket.listener;

import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TicketDlqListener {

    private final WebClient webClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    public TicketDlqListener(WebClient.Builder webClientBuilder, ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.webClient = webClientBuilder.build();
        this.redisTemplate = reactiveRedisTemplate;
    }

    @KafkaListener(topics = "ticketing-topic.DLQ", groupId = "ticket-dlq-group")
    public void consumeDlq(List<ReservationEvent> failedEvents){
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

        // 2. Redis 롤백 파이프라인과 Slack 전송 파이프라인 결합
        Flux.fromIterable(failedEvents)
                .flatMap(event -> {
                    String seatSetKey = TicketUtil.createPerformanceRedisKey(event.performanceId());
                    return redisTemplate.opsForSet().add(seatSetKey, String.valueOf(event.performanceSeatId()));
                })
                // Redis 처리가 모두 끝나면(.then), Slack 알림 전송을 시작함
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
