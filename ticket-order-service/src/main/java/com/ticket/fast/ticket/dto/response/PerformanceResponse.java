package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Performance;

import java.time.LocalDateTime;

public record PerformanceResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static PerformanceResponse fromEntity(Performance entity){
        return new PerformanceResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}
