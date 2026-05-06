package com.ticket.fast.ticket.service;

import com.ticket.fast.ticket.domain.SeatStatus;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RedisWarmUpService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final PerformanceSeatRepository performanceSeatRepository;

    public Mono<Void> warmUpSeats(Long performanceId){
        String key = TicketUtil.createPerformanceRedisKey(performanceId);

        return performanceSeatRepository.findAllByPerformanceIdAndStatus(performanceId, SeatStatus.AVAILABLE)
                .map(seat -> String.valueOf(seat.getId()))
                .collectList()
                .flatMap(seatIds -> {
                    if (seatIds.isEmpty()) return Mono.empty();
                    return redisTemplate.opsForSet().add(key, seatIds.toArray(new String[0]));
                })
                .then();
    }
}
