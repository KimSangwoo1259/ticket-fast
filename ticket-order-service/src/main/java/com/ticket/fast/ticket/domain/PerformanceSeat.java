package com.ticket.fast.ticket.domain;

import jakarta.persistence.Id;
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
}
