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
        String systemCommand = """
                너는 ticket-fast 서비스를 대표하는 친절하고 똑똑한 공연 추천 인공지능 상담원이야.
                    오직 아래 제공된 [공연 정보 컨텍스트]만을 바탕으로 유저의 질문에 답변해 줘.
                    만약 제공된 정보에 유저가 찾는 공연이 없거나 답을 알 수 없다면, 말을 지어내지 말고 
                    "죄송합니다, 관련된 공연 정보를 찾을 수 없습니다."라고 정중하게 답변해 줘.
                """;

        return builder
                .defaultSystem(systemCommand)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder().topK(3)
                                .similarityThreshold(0.7)
                                .build())
                        .build(),
                        MessageChatMemoryAdvisor.builder(chatMemory()).build())
                .build();
    }
}
