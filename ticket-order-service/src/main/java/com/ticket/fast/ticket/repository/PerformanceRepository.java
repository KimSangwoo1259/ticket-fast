package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.Performance;
import com.ticket.fast.ticket.repository.custom.PerformanceRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface PerformanceRepository extends R2dbcRepository<Performance, Long>, PerformanceRepositoryCustom {
    Flux<Performance> findByTitleContainingIgnoreCaseAndStartTimeBetween(String title, LocalDateTime startTimeAfter, LocalDateTime startTimeBefore, Pageable pageable);

    Mono<Long> countByTitleContainingIgnoreCaseAndStartTimeBetween(String title, LocalDateTime startTimeAfter, LocalDateTime startTimeBefore);
}
