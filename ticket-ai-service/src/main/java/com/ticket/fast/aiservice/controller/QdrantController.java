package com.ticket.fast.aiservice.controller;

import com.ticket.fast.aiservice.dto.PerformanceDto;
import com.ticket.fast.aiservice.service.DataIngestionService;
import com.ticket.fast.common.annotation.AdminOnly;
import com.ticket.fast.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RequestMapping("/api/ai")
@RequiredArgsConstructor
@RestController
public class QdrantController {

    private final DataIngestionService dataIngestionService;

    @AdminOnly
    @PostMapping("/admin/ingest")
    public Mono<ResponseEntity<ApiResponse<String>>> ingestPerformanceData(@RequestBody List<PerformanceDto> performanceList){
        return dataIngestionService.ingestPerformances(performanceList)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }
}
