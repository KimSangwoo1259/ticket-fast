package com.ticket.fast.ticket.dto.event;

public record ReservationEvent(
        Long userId,
        Long performanceId,
        Long performanceSeatId,
        String seatCode,
        Integer price

) {
}
