package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.Reservation;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface ReservationRepository extends R2dbcRepository<Reservation,Long> {

}
