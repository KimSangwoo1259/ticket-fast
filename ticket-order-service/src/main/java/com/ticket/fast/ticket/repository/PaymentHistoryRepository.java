package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.PaymentHistory;
import com.ticket.fast.ticket.repository.custom.PaymentHistoryRepositoryCustom;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface PaymentHistoryRepository extends R2dbcRepository<PaymentHistory, Long>, PaymentHistoryRepositoryCustom {

    Flux<PaymentHistory> findByIdIn(List<Long> neyIds);
}
