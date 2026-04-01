package com.ticket.fast.ticket.dto.request;

public record PaymentRequest(
        Integer amount,
        Long reservationId,
        String method

) {
}
