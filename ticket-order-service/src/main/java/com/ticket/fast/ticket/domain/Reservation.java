package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("reservation")
public class Reservation {

    @Id
    private final Long id;

    private final Long performanceId;
    private final Long userId;

    private final String seatCode;
    private final ReservationStatus status;
    private final LocalDateTime reservedAt;

    @Builder
    private Reservation(Long performanceId, Long userId, String seatCode, ReservationStatus status) {
        this.id = TsidUtil.nextLong();
        this.performanceId = performanceId;
        this.userId = userId;
        this.seatCode = seatCode;
        this.status = status;
        this.reservedAt = LocalDateTime.now();
    }
}
