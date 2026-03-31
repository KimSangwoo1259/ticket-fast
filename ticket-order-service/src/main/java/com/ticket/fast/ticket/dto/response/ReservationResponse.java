package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long performanceId,
        Long userId,
        String seatCode,
        Integer price,
        ReservationStatus status,
        LocalDateTime reservedAt

) {
    public static ReservationResponse fromEntity(Reservation entity){
        return new ReservationResponse(
                entity.getId(),
                entity.getPerformanceId(),
                entity.getUserId(),
                entity.getSeatCode(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getReservedAt()
        );
    }
}
