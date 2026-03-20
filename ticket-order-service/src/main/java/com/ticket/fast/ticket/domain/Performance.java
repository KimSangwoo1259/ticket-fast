package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;

@Getter
@Table("performance")
public class Performance {

    @Id
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer price;

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
