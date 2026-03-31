package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;


import java.time.LocalDateTime;

@Getter
@Table("performance")
public class Performance  {

    @Id
    private Long id;
    private String title;
    private String description;
    private PerformanceCategory category;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    @Builder
    private Performance(String title, String description, PerformanceCategory category, LocalDateTime startTime, LocalDateTime endTime){
        this.id = TsidUtil.nextLong();
        this.title = title;
        this.description = description;
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, String description, PerformanceCategory category, LocalDateTime startTime, LocalDateTime endTime){
        this.title = title;
        this.description = description;
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
