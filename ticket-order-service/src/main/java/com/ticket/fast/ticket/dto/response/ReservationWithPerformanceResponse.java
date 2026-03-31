package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Performance;
import com.ticket.fast.ticket.domain.PerformanceCategory;
import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationWithPerformanceResponse(
        Long reservationId,
        Long performanceId,
        Long userId,
        String title,
        PerformanceCategory category,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String seatCode,
        Integer price,
        ReservationStatus status,
        LocalDateTime reservedAt


) {
    public static ReservationWithPerformanceResponse fromEntity(Performance performance, Reservation reservation){
        return new ReservationWithPerformanceResponse(
                reservation.getId(),
                performance.getId(),
                reservation.getUserId(),
                performance.getTitle(),
                performance.getCategory(),
                performance.getStartTime(),
                performance.getEndTime(),
                reservation.getSeatCode(),
                reservation.getPrice(),
                reservation.getStatus(),
                reservation.getReservedAt()
        );
    }
}
