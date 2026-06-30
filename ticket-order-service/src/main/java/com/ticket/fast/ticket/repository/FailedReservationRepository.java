package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.FailedReservation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface FailedReservationRepository extends R2dbcRepository<FailedReservation,Long> {
}
