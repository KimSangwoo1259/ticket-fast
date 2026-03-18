package com.ticket.fast.ticket.dto.request;

public record ReservationCreateRequest(
        Long performanceId,
        String seatCode
) {
}
