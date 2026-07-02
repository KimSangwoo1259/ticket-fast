package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import com.ticket.fast.ticket.dto.event.ReservationEvent;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("failed_reservation")
public class FailedReservation {

    @Id
    private Long id;
    private Long performanceId;
    private Long performanceSeatId;
    private Long userId;
    private String failReason;
    private LocalDateTime failedAt;
    @Version
    private Long version;


    @Builder
    private FailedReservation(Long performanceId, Long performanceSeatId, Long userId, String failReason) {
        this.id = TsidUtil.nextLong();
        this.performanceId = performanceId;
        this.performanceSeatId = performanceSeatId;
        this.userId = userId;
        this.failReason = failReason;
        this.failedAt = LocalDateTime.now();
    }

    public static FailedReservation fromEvent(ReservationEvent event, String failedReason){
        return FailedReservation.builder()
                .performanceId(event.performanceId())
                .performanceSeatId(event.performanceSeatId())
                .userId(event.userId())
                .failReason(failedReason)
                .build();

    }

}
