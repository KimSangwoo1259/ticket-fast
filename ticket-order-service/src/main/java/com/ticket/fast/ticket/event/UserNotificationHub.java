package com.ticket.fast.ticket.event;


import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserNotificationHub {

    private final Map<Long, Sinks.Many<ServerSentEvent<String>>> userSinks = new ConcurrentHashMap<>();


    //유저가 최초로 SSE 개인 알림 연결을 맺을 때 (프론트엔드에서 호출)
    public Flux<ServerSentEvent<String>> subscribe(Long userId){
        Sinks.Many<ServerSentEvent<String>> sink =
                userSinks.computeIfAbsent(userId, key -> Sinks.many().unicast().onBackpressureBuffer());

        return sink.asFlux()
                .doOnCancel(() -> userSinks.remove(userId));
    }

    public void publishToUser(Long userId, String eventName, String data){
        Sinks.Many<ServerSentEvent<String>> sink = userSinks.get(userId);

        if (Objects.nonNull(sink)){
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                    .event(eventName)
                    .data(data)
                    .build();

            sink.emitNext(event, (singleType, emitResult)
                    -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED);
        }
    }
}
