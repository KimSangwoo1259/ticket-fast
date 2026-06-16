package com.ticket.fast.aiservice.service;

import com.ticket.fast.aiservice.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private final double threshHold = 0.65;
    private final int topK = 5;

    public Mono<ChatResponse> chatWithContext(
            String userMessage,
            String sessionId) {

        return Mono.fromCallable(() -> {

            List<Document> documents =
                    vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(userMessage)
                                    .topK(topK)
                                    .similarityThreshold(threshHold)
                                    .build());

            documents.forEach(doc ->
                    log.info("score={}, metadata={}",
                            doc.getScore(),
                            doc.getMetadata()));

            String context = documents.stream()
                    .map(doc -> String.format("""
                        공연 ID: %s
                        %s
                        """,
                            doc.getMetadata().get("performance_id"),
                            doc.getText()))
                    .collect(Collectors.joining("\n\n"));

            String answer = chatClient.prompt()
                    .system("""
                        아래 공연 정보만 참고해서 답변해.

                        %s
                        """.formatted(context))
                    .user(userMessage)
                    .advisors(a ->
                            a.param(ChatMemory.CONVERSATION_ID,
                                    sessionId))
                    .call()
                    .content();

            List<ChatResponse.RecommendedPerformance> performances =
                    documents.stream()
                            .map(doc -> new ChatResponse.RecommendedPerformance(
                                    Long.parseLong(
                                            doc.getMetadata()
                                                    .get("performance_id")
                                                    .toString()),
                                    doc.getMetadata()
                                            .get("title")
                                            .toString())
                            )
                            .toList();

            return new ChatResponse(
                    answer,
                    performances);
        });
    }


}
