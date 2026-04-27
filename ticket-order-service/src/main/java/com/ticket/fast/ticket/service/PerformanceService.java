package com.ticket.fast.ticket.service;

import com.ticket.fast.common.annotation.AdminOnly;
import com.ticket.fast.common.dto.AuthUser;
import com.ticket.fast.common.exception.BusinessException;
import com.ticket.fast.common.exception.ErrorCode;
import com.ticket.fast.ticket.domain.Performance;
import com.ticket.fast.ticket.domain.PerformanceSeat;
import com.ticket.fast.ticket.domain.SeatStatus;
import com.ticket.fast.ticket.dto.request.PerformanceCreateRequest;
import com.ticket.fast.ticket.dto.request.PerformanceSeatRequest;
import com.ticket.fast.ticket.dto.request.PerformanceUpdateRequest;
import com.ticket.fast.ticket.dto.response.PerformanceResponse;
import com.ticket.fast.ticket.dto.response.PerformanceSeatResponse;
import com.ticket.fast.ticket.dto.response.PerformanceWithSeatsResponse;
import com.ticket.fast.ticket.repository.PerformanceRepository;
import com.ticket.fast.ticket.repository.PerformanceSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final PerformanceSeatRepository performanceSeatRepository;

    // 공연 생성
    @AdminOnly
    @Transactional
    public Mono<PerformanceResponse> createPerformance(AuthUser authUser, PerformanceCreateRequest request) {
        return Mono.just(request)
                .filter(req -> !req.startTime().isAfter(req.endTime()))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.INVALID_TIME_REQUEST)))
                .map(req -> Performance.builder()
                        .title(req.title())
                        .description(req.description())
                        .startTime(req.startTime())
                        .endTime(req.endTime())
                        .build())
                .flatMap(performanceRepository::save)
                .doOnNext(saved -> log.info("공연 생성 성공 id {}, title {}", saved.getId(), saved.getTitle()))
                .map(PerformanceResponse::fromEntity);
    }



    // 공연 리스트 조회
    public Mono<Page<PerformanceResponse>> searchPerformance(String title, String category, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {


        return Mono.zip(
                        performanceRepository.searchPerformanceByCondition(title, category,startTime, endTime, pageable)
                                .map(PerformanceResponse::fromEntity).collectList(),
                        performanceRepository.countPerformanceByCondition(title, category,startTime, endTime)
                ).doOnNext(a -> log.info("공연 정보 검색 title {}, startTime {} endTime {}", title, startTime, endTime))
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));

    }

    @AdminOnly
    @Transactional
    public Flux<PerformanceSeatResponse> createPerformanceSeats(AuthUser authUser, Long performanceId,List<PerformanceSeatRequest> requests){
        return performanceRepository.existsById(performanceId)
                .filter(exist -> exist)
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)))
                .flatMapMany(exist -> performanceSeatRepository.saveAll(
                        Flux.fromIterable(requests) // List를 Flux로 변환
                                .map(req -> PerformanceSeat.builder()
                                        .performanceId(performanceId)
                                        .seatCode(req.seatCode())
                                        .status(SeatStatus.AVAILABLE)
                                        .price(req.price())
                                        .build())
                ))
                .map(PerformanceSeatResponse::fromEntity)
                .doOnComplete(() -> log.info("공연(ID:{}) 좌석 저장 완료", performanceId))
                .doOnError(e -> log.error("공연 좌석 저장 중 에러: {}", e.getMessage()));
    }

    public Mono<PerformanceWithSeatsResponse> getPerformanceWithSeat(Long performanceId){
        return Mono.zip(
                performanceRepository.findById(performanceId)
                        .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND))),
                performanceSeatRepository.findByPerformanceId(performanceId)
                        .map(PerformanceSeatResponse::fromEntity).collectList()

        ).map(tuple -> PerformanceWithSeatsResponse.fromEntity(tuple.getT1(), tuple.getT2()))
                .doOnSuccess(r -> log.info("공연 상세 조회 성공 공연 id {}",performanceId))
                .doOnError(e -> log.error("공연 상세 조회 중 오류 발생 {}",e.getMessage(),e));
    }


    @AdminOnly
    @Transactional
    public Mono<PerformanceResponse> updatePerformance(Long performanceId, PerformanceUpdateRequest request) {
        return Mono.just(request)
                .filter(req -> !req.startTime().isAfter(req.endTime()))
                .flatMap(req -> performanceRepository.findById(performanceId))
                .switchIfEmpty(Mono.error(new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND)))
                .flatMap(performance -> {
                    performance.update(
                            request.title(),
                            request.description(),
                            request.category(),
                            request.startTime(),
                            request.endTime()
                    );
                    return performanceRepository.save(performance);
                }).map(PerformanceResponse::fromEntity);
    }


}
