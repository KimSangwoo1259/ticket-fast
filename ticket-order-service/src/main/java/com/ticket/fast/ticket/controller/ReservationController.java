package com.ticket.fast.ticket.controller;

import com.ticket.fast.common.annotation.LoginUser;
import com.ticket.fast.common.dto.ApiResponse;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.ticket.dto.request.PaymentRequest;
import com.ticket.fast.ticket.dto.request.ReservationCreateRequest;
import com.ticket.fast.ticket.dto.response.PaymentResponse;
import com.ticket.fast.ticket.dto.response.ReservationResponse;
import com.ticket.fast.ticket.dto.response.ReservationWithPerformanceResponse;
import com.ticket.fast.ticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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


    @PostMapping("/v2")
    public Mono<ResponseEntity<ApiResponse<ReservationResponse>>> createReservationByRedis(@LoginUser AuthUser authUser,
                                                                                           @RequestBody ReservationCreateRequest request){
        return reservationService.createReservationByRedis(authUser, request).map(
                response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response))
        );
    }

    @PostMapping("/v3")
    public Mono<ResponseEntity<ApiResponse<ReservationResponse>>> createReservationByKafka(@LoginUser AuthUser authUser,
                                                                                    @RequestBody ReservationCreateRequest request){
        return reservationService.createReservationByRedisAndKafka(authUser, request).map(
                response -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response))
        );
    }




    @PostMapping("/payment")
    public Mono<ResponseEntity<ApiResponse<PaymentResponse>>> approvePayment(@LoginUser AuthUser authUser,
                                                                             @RequestBody PaymentRequest paymentRequest){
        return reservationService.approvePayment(authUser, paymentRequest)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping("/v2/payment")
    public Mono<ResponseEntity<ApiResponse<Void>>> approvePaymentByKafka(@LoginUser AuthUser authUser,
                                                                  @RequestBody PaymentRequest paymentRequest){
        return reservationService.approvePaymentByKafka(authUser, paymentRequest)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success(null))));
    }


    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Page<ReservationWithPerformanceResponse>>>> getMyReservations(@LoginUser AuthUser authUser,
                                                                                                         @PageableDefault(size = 10, page = 0) Pageable pageable){
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
