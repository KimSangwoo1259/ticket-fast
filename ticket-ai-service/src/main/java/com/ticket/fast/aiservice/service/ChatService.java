package com.ticket.fast.aiservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;



@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;


    public Mono<String> chatWithContext(String userMessage, String sessionId) {

        return Mono.fromCallable(() ->
                chatClient.prompt()
                        .user(userMessage)
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                        .call()
                        .content()
        );
    }
}
