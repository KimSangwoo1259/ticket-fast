package com.ticket.fast.ticket.domain;

import com.ticket.fast.common.util.TsidUtil;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.relational.core.mapping.Table;

@Table("performance_seat")
@Getter
public class PerformanceSeat {
    @Id
    private Long id;
    private Long performanceId;
    private String seatCode;
    private SeatStatus status;
    private Integer price;

    @Builder
    private PerformanceSeat(Long performanceId, String seatCode, SeatStatus status, Integer price) {
        this.id = TsidUtil.nextLong();
        this.performanceId = performanceId;
        this.seatCode = seatCode;
        this.status = status;
        this.price = price;
    }
}
