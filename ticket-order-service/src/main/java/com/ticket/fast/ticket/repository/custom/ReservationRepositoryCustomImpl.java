package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.common.util.TsidUtil;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Repository
public class ReservationRepositoryCustomImpl implements ReservationRepositoryCustom{
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> saveAllEventsWithIgnore(List<ReservationEvent> events) {
        if (CollectionUtils.isEmpty(events))
            return Mono.just(0L);

        String valuesClause = IntStream.range(0, events.size())
                .mapToObj(i -> String.format(
                        "(:id%d, :perfId%d, :userId%d, :seatCode%d, :price%d, 'CONFIRMED', :createdAt%d)",
                        i, i, i, i, i, i))
                .collect(Collectors.joining(", "));

        String sql = """
                INSERT IGNORE INTO reservation
                (id, performance_id, user_id, seat_code, price, status, created_at)
                VALUES %s
                """.formatted(valuesClause);

        DatabaseClient.GenericExecuteSpec executeSpec = databaseClient.sql(sql);

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < events.size(); i++){
            ReservationEvent event = events.get(i);
            executeSpec = executeSpec
                    .bind("id" + i, TsidUtil.nextLong())
                    .bind("perfId" + i, event.performanceId())
                    .bind("userId" + i, event.userId())
                    .bind("seatCode" + i, event.seatCode())
                    .bind("price" + i, event.price())
                    .bind("createdAt" + i, now);
        }

        return executeSpec.fetch().rowsUpdated();
    }
}
