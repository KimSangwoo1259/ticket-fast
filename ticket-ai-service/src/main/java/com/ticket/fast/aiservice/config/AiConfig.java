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
    public ChatClient chatClient(ChatClient.Builder builder) {

        String systemCommand = """
        너는 티켓패스트(TicketFast) 서비스를 대표하는 친절하고 전문적인 AI 예매 도우미야.

        제공된 공연 정보만을 기반으로 답변해.

        [답변 규칙]
        1. 공연 추천 시 공연명을 자연스럽게 언급해.
        2. 제공된 공연 정보 외의 내용을 지어내지 마.
        3. 관련 공연이 없다면 "죄송합니다, 관련된 공연 정보를 찾을 수 없습니다." 라고 답해.
        4. 사용자의 질문과 직접 관련된 공연만 답변해.
        5. 특정 스포츠를 물어보면 다른 종목은 제외해.
        """;

        return builder
                .defaultSystem(systemCommand)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory()).build()
                )
                .build();
    }
}
