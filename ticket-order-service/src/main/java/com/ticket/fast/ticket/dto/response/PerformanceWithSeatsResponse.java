package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.Performance;

import java.time.LocalDateTime;
import java.util.List;

public record PerformanceWithSeatsResponse(
        Long id,
        String title,
        String description,
        String venue,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<PerformanceSeatResponse> seatList
) {
    public static PerformanceWithSeatsResponse fromEntity(Performance performance, List<PerformanceSeatResponse> seatList){
        return new PerformanceWithSeatsResponse(
                performance.getId(),
                performance.getTitle(),
                performance.getDescription(),
                performance.getVenue(),
                performance.getStartTime(),
                performance.getEndTime(),
                seatList
        );
    }

}
