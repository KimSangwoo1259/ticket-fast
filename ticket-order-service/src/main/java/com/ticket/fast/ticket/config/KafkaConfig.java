package com.ticket.fast.ticket.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.StringJacksonJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate){
        // 실패 메시지를 기존토픽명.DLQ 로 전송하는 복구 뼈대 생성
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);


        //에러간격: 1초, 최대 재시도 횟수: 3
        //총 3번 시도후 실패하면 DLQ 토픽으로 던진 후 오프셋 상제 커밋
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);


        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        return errorHandler;
    }


    // 배치 리스너 활성화
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Object> kafkaListenerContainerFactory(
            ConsumerFactory<String,Object> consumerFactory
    ){
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.setBatchListener(true);
       // JSON 문자열을 객체로 변환해 주는 배치 컨버터
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(new StringJacksonJsonMessageConverter()));

        return factory;
    }
}
