package com.ticket.fast.aiservice.controller;

import com.ticket.fast.aiservice.dto.ChatResponse;
import com.ticket.fast.aiservice.service.ChatService;
import com.ticket.fast.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chat")
public class ChatController {

    private final ChatService chatService;
    private final VectorStore vectorStore;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<ChatResponse>>> chat(@RequestParam String message,
                                                                @RequestParam String sessionId){
        return chatService.chatWithContext(message,sessionId).map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @GetMapping("/debug")
    public List<Document> debug(@RequestParam String query) {

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)
                        .similarityThreshold(0.0)
                        .build()
        );

        docs.forEach(doc ->
                log.info("{}", doc));

        return docs;
    }


}
