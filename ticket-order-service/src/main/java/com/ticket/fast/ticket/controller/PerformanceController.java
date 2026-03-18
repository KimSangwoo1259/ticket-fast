package com.ticket.fast.ticket.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.ApiResponse;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.common.util.AuthConstant;
import com.ticket.fast.ticket.dto.request.PerformanceCreateRequest;
import com.ticket.fast.ticket.dto.response.PerformanceResponse;
import com.ticket.fast.ticket.repository.PerformanceRepository;
import com.ticket.fast.ticket.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/api/performance")
@RestController
public class PerformanceController {

    private final PerformanceService performanceService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<PerformanceResponse>>> createPerformance(@LoginUser AuthUser authUser,
                                                                                    @RequestBody PerformanceCreateRequest request){
        if (!"ADMIN".equals(authUser.role())){
            throw new BusinessException(ErrorCode.ADMIN_ADMIRE_ACTION);
        }

        return performanceService.createPerformance(request)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }
}
