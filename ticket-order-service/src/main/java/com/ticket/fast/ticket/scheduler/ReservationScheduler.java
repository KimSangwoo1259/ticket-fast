package com.ticket.fast.ticket.scheduler;

import com.ticket.fast.ticket.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationService reservationService;

//    @Scheduled(fixedDelay = 60000) //1분 간격으로
//    public void cleanupPendingReservations() {
//        log.info("만료된 예약 정리 스케줄러 가동");
//
//        reservationService.processExpiredReservations()
//                .subscribe(
//                        null,
//                        error -> log.error("스케줄러 작업 중 에러 발생: {}", error.getMessage(), error),
//                        () -> log.info("만료된 예약 정리 완료")
//                );
//
//    }
}
