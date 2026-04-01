package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.PaymentHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface PaymentHistoryRepository extends R2dbcRepository<PaymentHistory, Long> {

}
