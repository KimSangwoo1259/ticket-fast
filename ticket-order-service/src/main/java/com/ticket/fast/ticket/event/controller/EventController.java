package com.ticket.fast.ticket.event.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.ticket.event.PerformanceEventHub;
import com.ticket.fast.ticket.event.UserNotificationHub;
import com.ticket.fast.ticket.event.dto.SeatStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.ZoneId;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/event")
@RestController
public class EventController {

    private final PerformanceEventHub performanceEventHub;
    private final UserNotificationHub userNotificationHub;

    @GetMapping(value = "/performance/{performanceId}/seats",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SeatStatusEvent>> streamSeatStatus(@PathVariable Long performanceId) {
        return performanceEventHub.subscribe()
                .filter(event -> event.performanceId().equals(performanceId)) // 해당 공연 이벤트만 필터링
                .map(event -> ServerSentEvent.<SeatStatusEvent>builder()
                        .id(String.valueOf(event.updatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
                        .event("seat-update")
                        .data(event)
                        .build())
                .doOnSubscribe(sub -> log.info("SSE 연결 시작: 공연 ID {}", performanceId))
                .doOnTerminate(() -> log.info("SSE 연결 종료: 공연 ID {}", performanceId));

    }

    @GetMapping(value = "/sse/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamUserNotification(@LoginUser AuthUser authUser){
        return userNotificationHub.subscribe(authUser.userId())
                .doOnSubscribe(sub -> log.info("결제 알림 SSE 구독 시작: 유저 ID {}", authUser.userId()))
                .doOnCancel(() -> log.info("결제 알림 SSE 구독 종료: 유저 ID {}", authUser.userId()));
    }
}
