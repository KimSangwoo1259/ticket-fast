package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.PerformanceSeat;
import com.ticket.fast.ticket.domain.SeatStatus;

public record PerformanceSeatResponse(
        Long id,
        Long performanceId,
        String seatCode,
        SeatStatus status,
        Integer price

) {
    public static PerformanceSeatResponse fromEntity(PerformanceSeat entity){
        return new PerformanceSeatResponse(
                entity.getId(),
                entity.getPerformanceId(),
                entity.getSeatCode(),
                entity.getStatus(),
                entity.getPrice()
        );
    }
}
