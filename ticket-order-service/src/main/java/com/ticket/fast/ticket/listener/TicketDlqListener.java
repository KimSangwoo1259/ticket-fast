package com.ticket.fast.ticket.listener;

import com.ticket.fast.ticket.dto.ReservationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TicketDlqListener {

    private final WebClient webClient;

    @Value("${slack.webhook.url}")
    private String slackWebhookUrl;

    public TicketDlqListener(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
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

        webClient.post()
                .uri(slackWebhookUrl)
                .bodyValue(Map.of("text", slackMessage))
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("슬랙 장애 알림 전송 완료"))
                .doOnError(e -> log.error("슬랙 장애 알림 전송 중 에러 발생 {}", e.getMessage(), e))
                .subscribe();
    }
}
