package com.ticket.fast.ticket.dto.request;


import java.util.List;

public record PerformanceSeatCreateRequest(
        Long performanceId,
        List<PerformanceSeatRequest> requestSeats

) {
}
