package com.ticket.fast.aiservice.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {


    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, VectorStore vectorStore){
        String systemCommand = "너는 티켓패스트(TicketFast)의 친절하고 전문적인 AI 예매 도우미야. 제공된 공연 정보를 바탕으로 유저에게 딱 맞는 공연을 추천해줘. 정보가 없으면 모른다고 솔직하게 대답해.";

        return builder
                .defaultSystem(systemCommand)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(3).build())
                        .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory()).build())
                .build();
    }
}
