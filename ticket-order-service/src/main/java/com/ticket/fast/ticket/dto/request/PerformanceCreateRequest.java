package com.ticket.fast.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record PerformanceCreateRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        String title,
        String description,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
