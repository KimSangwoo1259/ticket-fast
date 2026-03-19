package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.Reservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReservationRepository extends R2dbcRepository<Reservation,Long> {
    Flux<Reservation> findByUserId(Long userId, Pageable pageable);

    Mono<Long> countByUserId(Long userId);

}
