package com.ticket.fast.ticket.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.ApiResponse;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.ticket.dto.request.PerformanceCreateRequest;
import com.ticket.fast.ticket.dto.request.PerformanceSeatRequest;
import com.ticket.fast.ticket.dto.request.PerformanceUpdateRequest;
import com.ticket.fast.ticket.dto.response.PerformanceResponse;
import com.ticket.fast.ticket.dto.response.PerformanceSeatResponse;
import com.ticket.fast.ticket.dto.response.PerformanceWithSeatsResponse;
import com.ticket.fast.ticket.service.PerformanceService;
import com.ticket.fast.ticket.service.RedisWarmUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/performance")
@RestController
public class PerformanceController {

    private final PerformanceService performanceService;
    private final RedisWarmUpService warmUpService;

    //공연 생성
    @PostMapping
    public Mono<ResponseEntity<ApiResponse<PerformanceResponse>>> createPerformance(@LoginUser AuthUser authUser,
                                                                                    @Valid @RequestBody PerformanceCreateRequest request){
        return performanceService.createPerformance(authUser,request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<Page<PerformanceResponse>>>> searchPerformances(
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam LocalDateTime startTime,
            @RequestParam LocalDateTime endTime,
            @PageableDefault(size = 10, page = 0) Pageable pageable
    ) {
        return performanceService.searchPerformance(title,category, startTime, endTime, pageable)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @GetMapping("/{performanceId}")
    public Mono<ResponseEntity<ApiResponse<PerformanceWithSeatsResponse>>> getPerformanceWithSeats(@PathVariable Long performanceId){
        return performanceService.getPerformanceWithSeat(performanceId)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PutMapping("/{performanceId}")
    public Mono<ResponseEntity<ApiResponse<PerformanceResponse>>> updatePerformance(@PathVariable Long performanceId,
                                                                                    @LoginUser AuthUser authUser,
                                                                                    @Valid @RequestBody PerformanceUpdateRequest request) {
        return performanceService.updatePerformance(performanceId, request)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    //공연 좌석 생성
    @PostMapping("/seat/{performanceId}")
    public Mono<ResponseEntity<ApiResponse<List<PerformanceSeatResponse>>>> createPerformanceSeats(@LoginUser AuthUser authUser,
                                                                                             @PathVariable Long performanceId,
                                                                                             @RequestBody List<PerformanceSeatRequest> request){
        return performanceService.createPerformanceSeats(authUser, performanceId,request)
                .collectList()
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }


    @PostMapping("/warmup/{performanceId}")
    public Mono<Void> warmUp(@PathVariable Long performanceId){
        return warmUpService.warmUpSeats(performanceId);
    }

    @PostMapping("/warmup/batch")
    public Mono<Void> warmup(@RequestBody List<Long> performanceIds){
        return warmUpService.warmUpBatch(performanceIds);
    }




}
