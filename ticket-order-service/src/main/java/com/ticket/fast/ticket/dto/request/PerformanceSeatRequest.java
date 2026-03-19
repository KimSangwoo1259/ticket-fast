package com.ticket.fast.ticket.dto.request;

public record PerformanceSeatRequest(
        String seatCode,
        Integer price
) {
}
