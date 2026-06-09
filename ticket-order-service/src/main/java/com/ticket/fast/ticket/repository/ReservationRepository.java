package com.ticket.fast.ticket.repository;

import com.ticket.fast.ticket.domain.Reservation;
import com.ticket.fast.ticket.domain.ReservationStatus;
import com.ticket.fast.ticket.repository.custom.ReservationRepositoryCustom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ReservationRepository extends R2dbcRepository<Reservation,Long>, ReservationRepositoryCustom {
    Flux<Reservation> findByUserId(Long userId, Pageable pageable);

    Mono<Long> countByUserId(Long userId);

    @Query("""
            
                        UPDATE reservation r
                        JOIN performance_seat s 
                            ON r.performance_id = s.performance_id 
                            AND r.seat_code = s.seat_code 
                        JOIN performance p 
                            ON r.performance_id = p.id 
                        SET 
                            r.status = 'CANCELLED',
                            r.cancelled_at = NOW(),
                            r.version = r.version + 1,
                            s.status = 'AVAILABLE',
                            s.version = s.version + 1 
                        WHERE
                            r.id = :reservationId 
                            AND r.status = 'CONFIRMED'              -- 2. 이미 취소된 건 중복 처리 방지
                            AND p.start_time > DATE_ADD(NOW(), INTERVAL 10 MINUTE) -- 3. 공연 시작 10분 전 체크
            """)
    @Modifying
    Mono<Long> cancelReservation(Long reservationId);

    Flux<Reservation> findTop100ByStatusAndCreatedAtBefore(ReservationStatus status, LocalDateTime createdAtBefore);
}
