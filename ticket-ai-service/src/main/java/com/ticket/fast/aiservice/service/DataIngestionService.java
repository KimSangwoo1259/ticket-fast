package com.ticket.fast.aiservice.service;

import com.ticket.fast.aiservice.dto.PerformanceDto;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
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
                    String content = String.format("""
                                    공연명: %s
                                    
                                    설명: %s
                                    
                                    장소: %s
                                    """,
                            performance.title(),
                            performance.description(),
                            performance.venue()
                    );

                    Map<String, Object> metadata = createMetadataMap(performance);
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

    @NotNull
    private static Map<String, Object> createMetadataMap(PerformanceDto performance) {
        Map<String, Object> metadata = new HashMap<>();

        if (performance.id() != null) {
            metadata.put("performance_id", performance.id());
        }

        if (performance.category() != null) {
            metadata.put("category", performance.category());
        }
        if (performance.title() != null){
            metadata.put("title", performance.title());
        }

        if (performance.venue() != null) {
            metadata.put("venue", performance.venue());
        }
        if (performance.startTime() != null) {
            metadata.put("startTime", performance.startTime().toLocalDate().toString());
        }
        if (performance.endTime() != null) {
            metadata.put("endTime", performance.endTime().toLocalDate().toString());
        }
        return metadata;
    }
}
