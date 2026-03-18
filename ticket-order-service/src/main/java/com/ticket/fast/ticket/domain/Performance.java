package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;

@Getter
@Table("performance")
public class Performance {

    @Id
    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Integer price;

    @Builder
    private Performance(String title, String description, LocalDateTime startTime, LocalDateTime endTime, Integer price){
        this.id = TsidUtil.nextLong();
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.price = price;
    }
}
