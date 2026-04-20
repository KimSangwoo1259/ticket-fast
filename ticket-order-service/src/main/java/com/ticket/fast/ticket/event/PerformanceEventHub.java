package com.ticket.fast.ticket.event;

import com.ticket.fast.ticket.event.dto.SeatStatusEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class PerformanceEventHub {
    // 여러 구독자에게 이벤트를 동시에 뿌려주는 통로 (가장 최근의 10개 이벤트 캐싱)
    private final Sinks.Many<SeatStatusEvent> seatSink =
            Sinks.many().multicast().onBackpressureBuffer(10);

    // 이벤트를 밖으로 쏴줄 때 (Flux로 변환)
    public Flux<SeatStatusEvent> subscribe() {
        return seatSink.asFlux();
    }

    // 새로운 이벤트가 발생했을 때 (예매 성공 시 호출)
    public void publish(SeatStatusEvent event){
        seatSink.tryEmitNext(event);
        ///
    }
}
