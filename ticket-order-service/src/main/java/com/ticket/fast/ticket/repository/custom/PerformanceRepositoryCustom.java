package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.ticket.domain.Performance;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface PerformanceRepositoryCustom {
    Flux<Performance> searchPerformanceByCondition(String title, String category, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Mono<Long> countPerformanceByCondition(String title, String category, LocalDateTime startTime, LocalDateTime endTime);
}
