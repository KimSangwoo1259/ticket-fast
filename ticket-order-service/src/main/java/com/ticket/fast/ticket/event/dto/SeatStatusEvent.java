package com.ticket.fast.ticket.event.dto;

import com.ticket.fast.ticket.domain.SeatStatus;

import java.time.LocalDateTime;

public record SeatStatusEvent(
        Long performanceId,
        String seatCode,
        SeatStatus status,
        LocalDateTime updatedAt


) {
}
