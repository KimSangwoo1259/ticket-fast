package com.ticket.fast.ticket.repository.custom;

import com.ticket.fast.ticket.domain.Performance;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@RequiredArgsConstructor
@Repository
public class PerformanceRepositoryCustomImpl implements PerformanceRepositoryCustom{
    private final R2dbcEntityTemplate entityTemplate;

    @Override
    public Flux<Performance> searchPerformanceByCondition(String title, String category, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Criteria criteria = buildCriteria(title, category, startTime, endTime);

        return entityTemplate.select(Performance.class)
                .from("performance")
                .matching(Query.query(criteria).with(pageable))
                .all();

    }

    @Override
    public Mono<Long> countPerformanceByCondition(String title, String category, LocalDateTime startTime, LocalDateTime endTime) {
        Criteria criteria = buildCriteria(title, category, startTime, endTime);

        return entityTemplate.count(Query.query(criteria), Performance.class);
    }

    private static Criteria buildCriteria(String title, String category, LocalDateTime startTime, LocalDateTime endTime) {
        Criteria criteria = getCriteria();

        if (StringUtils.hasText(title)){
            criteria = criteria.and("title").like("%" + title + "%").ignoreCase(true);
        }
        if (StringUtils.hasText(category)){
            criteria = criteria.and("category").is(category);
        }
        if (Objects.nonNull(startTime) && Objects.nonNull(endTime)){
            criteria = criteria.and("startTime").between(startTime, endTime);
        }
        return criteria;
    }
    private static Criteria getCriteria(){
        return Criteria.empty().and("deleted_at").isNull();
    }
}
