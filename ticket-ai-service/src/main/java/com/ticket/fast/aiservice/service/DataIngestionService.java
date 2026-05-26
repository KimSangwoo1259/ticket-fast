package com.ticket.fast.aiservice.service;

import com.ticket.fast.aiservice.dto.PerformanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DataIngestionService {
    private final VectorStore vectorStore;


    public Mono<String> ingestPerformances(List<PerformanceDto> performanceList) {
        if (CollectionUtils.isEmpty(performanceList)) {
            return Mono.empty();
        }

         Flux.fromIterable(performanceList)
                .map(performance -> {
                    String content = String.format("공연 정보 안내 \n 공연 ID: %s \n공연명: %s\n 카테고리: %s \n 공연 장소: %s \n 공연 설명: %s\n 시작일시: %s\n 종료일시: %s",
                            performance.id(),performance.title(), performance.category(), performance.venue(),
                            performance.description(), performance.startTime(), performance.endTime());

                    Map<String, Object> metadata = new HashMap<>();

                    // 나중에 유사도 검색 결과로 나온 ID를 통해 DB(R2DBC/Redis)에서 매핑해 올 식별자
                    if (performance.id() != null) {
                        metadata.put("performance_id", performance.id());
                    }

                    // "뮤지컬 중에서만 검색", "콘서트 중에서만 검색" 등 카테고리 필터링용 (Enum인 경우 .name() 처리)
                    if (performance.category() != null) {
                        metadata.put("category", performance.category());

                    }

                    // "상암 월드컵경기장 공연만 필터링" 등 특정 핫플레이스나 장소 기반 필터링용
                    if (performance.venue() != null) {
                        metadata.put("venue", performance.venue());
                    }

                    return new Document(content, metadata);
                })
                .buffer(5)
                .delayElements(Duration.ofSeconds(5))
                .flatMap(batchDocuments ->
                        Mono.fromRunnable(() -> vectorStore.accept(batchDocuments))
                                .subscribeOn(Schedulers.boundedElastic()), 1)
                // 🚨 핵심: .subscribe()를 붙여서 이 무거운 흐름을 메인 스레드와 분리해 백그라운드로 던집니다!
                .subscribe(
                        null,
                        error -> System.err.println("❌ 백그라운드 주입 중 에러 발생: " + error.getMessage()),
                        () -> System.out.println("✅ 169개 전체 데이터 벡터 DB 안착 완료! 🎉")
                );

        // 💡 작업은 백그라운드에서 돌기 시작했고, 유저에게는 게이트웨이가 지치기 전에 0.1초 만에 응답을 줍니다.
        return Mono.just("총 " + performanceList.size() + "개의 공연 데이터 주입을 백그라운드에서 시작했습니다. 로그를 확인하세요!");
    }
}
