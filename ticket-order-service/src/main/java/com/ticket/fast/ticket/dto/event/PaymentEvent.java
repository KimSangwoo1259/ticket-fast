package com.ticket.fast.ticket.dto.event;


public record PaymentEvent(
        Long userId,
        Long reservationId,
        Integer amount,
        String method
) {

}
