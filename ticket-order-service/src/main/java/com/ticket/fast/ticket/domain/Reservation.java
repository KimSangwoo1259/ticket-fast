package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("reservation")
public class Reservation  {

    @Id
    private Long id;

    private Long performanceId;
    private Long userId;

    private String seatCode;
    private Integer price;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reservedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime expiredAt;

    @Version
    private Long version;

    @Builder
    private Reservation(Long performanceId, Long userId, String seatCode,Integer price, ReservationStatus status) {
        this.id = TsidUtil.nextLong();
        this.performanceId = performanceId;
        this.userId = userId;
        this.seatCode = seatCode;
        this.price = price;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public void expire() {
        this.expiredAt = LocalDateTime.now();
        this.status = ReservationStatus.EXPIRED;
    }

}
