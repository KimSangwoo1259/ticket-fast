package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.ticket.dto.ReservationEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ReservationRepositoryCustom {

    Mono<Long> saveAllEventsWithIgnore(List<ReservationEvent> events);
}
