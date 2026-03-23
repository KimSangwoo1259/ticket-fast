package com.ticket.fast.ticket.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.ApiResponse;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;
import com.ticket.fast.ticket.dto.response.ReservationResponse;
import com.ticket.fast.ticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RequestMapping("/api/reservation")
@RestController
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ReservationResponse>>> createReservation(@LoginUser AuthUser authUser,
                                                                                    @RequestBody ReservationCreateRequest request){
        return reservationService.createReservation(authUser, request).map(
                response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response))
        );
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Page<ReservationResponse>>>> getMyReservations(@LoginUser AuthUser authUser,
                                                                                          Pageable pageable){
        return reservationService.getMyReservations(authUser, pageable)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @DeleteMapping("/{reservationId}")
    public Mono<ResponseEntity<ApiResponse<ReservationResponse>>> cancelReservation(@LoginUser AuthUser authUser,
                                                                                    @PathVariable Long reservationId){
        return reservationService.cancelReservation(authUser, reservationId)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }
}
