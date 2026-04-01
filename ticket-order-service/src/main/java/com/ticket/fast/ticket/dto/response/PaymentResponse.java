package com.ticket.fast.ticket.dto.response;

import com.ticket.fast.ticket.domain.PaymentHistory;
import com.ticket.fast.ticket.domain.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long reservationId,
        Integer amount,
        PaymentStatus status,
        String method,
        LocalDateTime createdAt

) {
    public static PaymentResponse fromEntity(PaymentHistory entity){
        return new PaymentResponse(
                entity.getId(),
                entity.getReservationId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getMethod(),
                entity.getCreatedAt()
        );
    }
}
