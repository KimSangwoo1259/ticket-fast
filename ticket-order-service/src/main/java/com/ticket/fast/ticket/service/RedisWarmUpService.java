package com.ticket.fast.ticket.service;

import com.ticket.fast.ticket.domain.SeatStatus;
import com.ticket.fast.ticket.dto.SeatInfo;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RedisWarmUpService {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final PerformanceSeatRepository performanceSeatRepository;
    private final ObjectMapper objectMapper;

    public Mono<Void> warmUpSeats(Long performanceId){
        String seatSetKey = TicketUtil.createPerformanceRedisKey(performanceId);
        String seatDetailKey = TicketUtil.createDetailKey(seatSetKey);

        return performanceSeatRepository.findAllByPerformanceIdAndStatus(performanceId, SeatStatus.AVAILABLE)
                .flatMap(seat -> {
                    String seatId = String.valueOf(seat.getId());

                    SeatInfo info = new SeatInfo(seat.getSeatCode(), seat.getPrice());

                    return Mono.zip(
                            redisTemplate.opsForSet().add(seatSetKey, seatId),
                            redisTemplate.opsForHash().put(seatDetailKey, seatId, objectMapper.writeValueAsString(info))
                    );
                })
                .then();
    }
}
