package com.ticket.fast.ticket.dto.request;

import com.ticket.fast.ticket.domain.PerformanceCategory;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record PerformanceUpdateRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        String title,
        String description,
        PerformanceCategory category,
        LocalDateTime startTime,
        LocalDateTime endTime

) {
}
