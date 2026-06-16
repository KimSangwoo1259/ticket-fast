package com.ticket.fast.aiservice.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<RecommendedPerformance> performances

) {
    public record RecommendedPerformance(
            Long performanceId,
            String title

    ) {

    }
}
