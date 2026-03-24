package com.ticket.fast.ticket.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PerformanceSeatRequest(
        String seatCode,

        @PositiveOrZero(message = "공연 가격은 음수가 될 수 없습니다.")
        @NotNull(message = "값을 입력해 주세요.")
        Integer price
) {
}
