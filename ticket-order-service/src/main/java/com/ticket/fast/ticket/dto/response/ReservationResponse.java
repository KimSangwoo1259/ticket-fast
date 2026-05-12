package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;
import com.ticket.fast.ticket.dto.SeatInfo;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;

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
    public static ReservationResponse pending(Long userId, ReservationCreateRequest request, SeatInfo seatInfo) {
        return new ReservationResponse(
                null,
                request.performanceId(),
                userId,
                seatInfo.seatCode(),
                seatInfo.price(),
                ReservationStatus.PENDING,
                LocalDateTime.now()
        );
    }
}
