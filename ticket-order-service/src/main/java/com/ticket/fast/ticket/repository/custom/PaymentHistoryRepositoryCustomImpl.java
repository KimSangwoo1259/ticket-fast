package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.ticket.domain.PaymentHistory;
import com.ticket.fast.ticket.dto.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class PaymentHistoryRepositoryCustomImpl implements PaymentHistoryRepositoryCustom{

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> saveAllEventsWithIgnore(List<PaymentHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return Mono.just(0L);
        }

        StringBuilder sql = new StringBuilder(
                "INSERT IGNORE INTO payment_history " +
                        "(id, user_id, reservation_id, amount, status, method, created_at) VALUES "
        );

        for (int i = 0; i < histories.size(); i++) {
            sql.append(String.format(
                    "(:id%d, :userId%d, :resId%d, :amount%d, :status%d, :method%d, :createdAt%d)",
                    i, i, i, i, i, i, i
            ));
            if (i < histories.size() - 1) {
                sql.append(", ");
            }
        }

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql.toString());

        for (int i = 0; i < histories.size(); i++) {
            PaymentHistory history = histories.get(i);

            spec = spec.bind("id" + i, history.getId())
                    .bind("userId" + i, history.getUserId())
                    .bind("resId" + i, history.getReservationId())
                    .bind("amount" + i, history.getAmount())
                    .bind("status" + i, history.getStatus().name()) // Enum 타입의 경우 String 변환
                    .bind("method" + i, history.getMethod())
                    .bind("createdAt" + i, history.getCreatedAt());
        }

        return spec.fetch().rowsUpdated();
    }
}

