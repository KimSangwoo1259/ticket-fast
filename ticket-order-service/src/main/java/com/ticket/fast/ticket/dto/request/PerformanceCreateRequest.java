package com.ticket.fast.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record PerformanceCreateRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        String title,
        String description,
        LocalDateTime startTime,
        LocalDateTime endTime,

        @PositiveOrZero(message = "공연 가격은 음수가 될 수 없습니다.")
        @NotNull(message = "값을 입력해 주세요.")
        Integer price
) {
}
