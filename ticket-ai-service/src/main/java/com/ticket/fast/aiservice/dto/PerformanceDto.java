package com.ticket.fast.aiservice.dto;

import java.time.LocalDateTime;

public record PerformanceDto(
        Long id,
        String title,
        String description,
        String venue,
        String category,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
