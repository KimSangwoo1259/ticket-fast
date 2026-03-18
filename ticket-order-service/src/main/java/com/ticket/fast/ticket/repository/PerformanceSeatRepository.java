package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.PerformanceSeat;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface PerformanceSeatRepository extends R2dbcRepository<PerformanceSeat,Long> {
    Mono<PerformanceSeat> findByPerformanceIdAndSeatCode(Long performanceId, String seatCode);

    @Modifying
    @Query("UPDATE performance_seats SET status = 'RESERVED' WHERE id = :id AND status = 'AVAILABLE'")
    Mono<Integer> reserveSeat(Long id);
}
