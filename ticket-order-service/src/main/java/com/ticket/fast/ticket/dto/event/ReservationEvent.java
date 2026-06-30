package com.ticket.fast.ticket.dto.event;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;

public record ReservationEvent(
        Long userId,
        Long performanceId,
        Long performanceSeatId,
        String seatCode,
        Integer price

) {
    public static Reservation toEntity(ReservationEvent event){
        return Reservation.builder()
                .performanceId(event.performanceId())
                .userId(event.userId())
                .seatCode(event.seatCode())
                .price(event.price())
                .status(ReservationStatus.PENDING)
                .build();
    }
}
