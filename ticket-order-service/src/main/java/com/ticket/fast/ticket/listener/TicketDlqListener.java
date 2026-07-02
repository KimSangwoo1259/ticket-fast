package com.ticket.fast.ticket.listener;

import com.ticket.fast.ticket.domain.FailedReservation;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import com.ticket.fast.ticket.repository.FailedReservationRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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


    @KafkaListener(
            topics = "ticketing-topic.DLQ",
            groupId = "ticket-dlq-group-test",
            containerFactory = "singleKafkaListenerContainerFactory")
    public void consumeDlq(
            @Payload ReservationEvent failedEvent,
            // required = false를 주어 헤더가 없어도 메서드가 실행되도록
            @Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            @Header(value = KafkaHeaders.DLT_EXCEPTION_FQCN, required = false) String exceptionType) {



        log.error("Error detected! Failed Data enqueued in DLQ: 좌석 ID {}", failedEvent.performanceSeatId());

        // 에러 사유 조합 (헤더가 null일 경우를 대비한 방어 로직)
        String exactReason = (exceptionType != null ? exceptionType : "UnknownException") + ": "
                + (exceptionMessage != null ? exceptionMessage : "No exception message provided");

        String slackMessage = String.format("""
                        *[티켓 시스템  장애 알림]*
                      • *내용:* 메인 컨슈머에서 처리에 실패하여 DLQ로 격리되었습니다.
                      • *장애 의심 좌석 ID:* %s
                      • *에러 사유:* %s
                      • *조치 요망:* 즉시 서버 로그를 확인하고 데이터를 수동 복구해 주세요.
                    """,
                failedEvent.performanceSeatId(), exactReason);

        FailedReservation failedReservation = FailedReservation.fromEvent(failedEvent, exactReason);


        String seatSetKey = TicketUtil.createPerformanceRedisKey(failedEvent.performanceId());

        redisTemplate.opsForSet().add(seatSetKey, String.valueOf(failedEvent.performanceSeatId()))
                .then(
                        failedReservationRepository.save(failedReservation)
                                .onErrorResume(e -> {
                                    log.error("실패 이력 DB 저장 실패 {}", e.getMessage(), e);
                                    return Mono.empty();
                                })
                )
                .then(
                        webClient.post()
                                .uri(slackWebhookUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("text", slackMessage))
                                .retrieve()
                                .toBodilessEntity()
                                .onErrorResume(e -> {
                                    log.error("슬랙 알림 실패 {}",e.getMessage(),e);
                                    return Mono.empty();
                                })
                )
                .doOnSuccess(response -> log.info("Redis 롤백 및 슬랙 알림 전송 완료"))
                .doOnError(e -> log.error("DLQ 후속 처리 중 크리티컬 에러 발생: {}", e.getMessage(), e))
                .block(); // Spring Kafka 오프셋 커밋을 위해 동기 블로킹 유지
    }
}
