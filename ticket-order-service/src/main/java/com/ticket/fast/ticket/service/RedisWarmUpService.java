package com.ticket.fast.ticket.service;

import com.ticket.fast.ticket.domain.SeatStatus;
import com.ticket.fast.ticket.dto.SeatInfo;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import com.ticket.fast.ticket.util.TicketUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
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

    public Mono<Void> warmUpBatch(List<Long> performanceIds) {
        return Flux.fromIterable(performanceIds)
                .flatMap(performanceId -> {
                    String seatSetKey = TicketUtil.createPerformanceRedisKey(performanceId);
                    String seatDetailKey = TicketUtil.createDetailKey(seatSetKey);

                    return performanceSeatRepository.findAllByPerformanceIdAndStatus(performanceId, SeatStatus.AVAILABLE)
                            .flatMap(seat -> {
                                String seatId = String.valueOf(seat.getId());
                                SeatInfo info = new SeatInfo(seat.getSeatCode(), seat.getPrice());

                                // 1. JSON 변환 중 발생하는 예외를 Mono.error로 전환
                                return Mono.fromCallable(() -> objectMapper.writeValueAsString(info))
                                        .flatMap(jsonInfo -> Mono.zip(
                                                redisTemplate.opsForSet().add(seatSetKey, seatId),
                                                redisTemplate.opsForHash().put(seatDetailKey, seatId, jsonInfo)
                                        ))
                                        .onErrorResume(e -> {
                                            log.error("JSON 변환 실패 - SeatID: {}, Error: {}", seatId, e.getMessage());
                                            return Mono.empty(); // 에러 발생 시 해당 좌석은 건너뜀
                                        });
                            });
                }, 5) // 2. 동시에 처리할 공연 수 제한 (t3.small 자원 보호)
                .then();
    }
}
