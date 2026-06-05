package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("payment_history")
public class PaymentHistory {

    @Id
    private Long id;

    private Long userId;

    private Long reservationId;

    private Integer amount;

    private PaymentStatus status;

    private String method;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    @Builder
    private PaymentHistory(Long userId,
                           Long reservationId,
                           Integer amount,
                           PaymentStatus status,
                           String method) {

        this.id = TsidUtil.nextLong();
        this.userId = userId;
        this.reservationId = reservationId;
        this.amount = amount;
        this.status = status;
        this.method = method;
        this.createdAt = LocalDateTime.now();
    }

}
