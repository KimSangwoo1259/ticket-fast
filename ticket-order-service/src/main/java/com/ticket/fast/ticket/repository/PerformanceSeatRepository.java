package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.PerformanceSeat;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PerformanceSeatRepository extends R2dbcRepository<PerformanceSeat,Long> {
    Mono<PerformanceSeat> findByPerformanceIdAndSeatCode(Long performanceId, String seatCode);

    Flux<PerformanceSeat> findByPerformanceId(Long performanceId);


    //
    @Modifying
    @Query("""
        UPDATE performance_seat 
        SET status = 'RESERVED', version = version + 1 
        WHERE id = :id AND status = 'AVAILABLE'
    """)
    Mono<Integer> reserveSeat(Long id);
}
