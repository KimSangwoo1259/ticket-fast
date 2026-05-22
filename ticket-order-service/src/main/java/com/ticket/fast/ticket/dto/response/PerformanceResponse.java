package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Performance;
import com.ticket.fast.ticket.domain.PerformanceCategory;

import java.time.LocalDateTime;

public record PerformanceResponse(
        Long id,
        String title,
        String description,
        String venue,
        PerformanceCategory category,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public static PerformanceResponse fromEntity(Performance entity){
        return new PerformanceResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getVenue(),
                entity.getCategory(),
                entity.getStartTime(),
                entity.getEndTime()
        );
    }
}
