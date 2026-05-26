package com.ticket.fast.aiservice.controller;

import com.ticket.fast.aiservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/chat")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public Mono<String> chat(@RequestParam String message){
        return chatService.chatWithContext(message);
    }
}
