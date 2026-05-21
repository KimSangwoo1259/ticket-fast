package com.ticket.fast.aiservice;

import com.ticket.fast.aiservice.dto.PerformanceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class DataIngestionService {
    private final VectorStore vectorStore;


    public Mono<Void> ingestPerformances(List<PerformanceDto> performanceList){
        return Mono.fromRunnable(() -> {
                    List<Document> documents = performanceList.stream()
                            .map(performance -> {
                                String content = String.format("공연 정보 안내 \n 공연명: %s\n 카테고리: %s \n 공연 설명: %s\n 시작일시: %s\n 종료일시: %s"
                                        , performance.title(), performance.category(), performance.description(), performance.startTime(), performance.endTime());
                                Map<String, Object> metadata = Map.of(
                                        "performance_id", performance.id(),
                                        "category", performance.category()
                                );

                                return new Document(content, metadata);

                            }).toList();

                    vectorStore.accept(documents);
                }).subscribeOn(Schedulers.boundedElastic())
                .then();
    }

}
