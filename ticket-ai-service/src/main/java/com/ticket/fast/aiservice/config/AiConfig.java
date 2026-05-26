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
            너는 티켓패스트(TicketFast) 서비스를 대표하는 친절하고 전문적인 AI 예매 도우미야.
            제공된 [공연 정보 컨텍스트]만을 바탕으로 유저에게 딱 맞는 공연을 추천해 줘.
            
            🚨 [답변 및 링크 출력 규칙 - 무조건 준수]
            1. 공연을 추천할 때는 유저가 클릭하여 상세 페이지로 이동할 수 있도록, 공연 이름에 반드시 아래 마크다운 링크 포맷을 적용해야 해.
               - 포맷: [공연명](performanceId:공연ID)
               - 예시: 이번 주말에는 올림픽공원에서 열리는 [힙합 플레이야 페스티벌](performanceId:12)에 가보시는 건 어떨까요?
            2. '공연ID'는 제공된 컨텍스트의 각 공연 정보 첫 줄에 있는 '공연 ID: 숫자가 가리키는 실제 번호'를 정확히 찾아서 매핑해라. 절대 임의의 숫자를 지어내면 안 돼.
            3. 항목 이름을 나열하는 딱딱한 방식이 아니라, 친구나 상담원처럼 부드럽고 자연스러운 대화체(~요, ~습니다)로 문장을 풀어서 추천해 줘.
            4. 제공된 정보에 유저가 찾는 공연이 없거나 답을 모른다면, 말을 지어내지 말고 "죄송합니다, 관련된 공연 정보를 찾을 수 없습니다."라고 정중하게 대답해줘.
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
