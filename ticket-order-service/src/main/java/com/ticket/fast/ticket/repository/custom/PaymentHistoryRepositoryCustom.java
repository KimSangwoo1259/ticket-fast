package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.ticket.domain.PaymentHistory;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaymentHistoryRepositoryCustom {

    Mono<Long> saveAllEventsWithIgnore(List<PaymentHistory> histories);
}
