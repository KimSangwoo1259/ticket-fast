package com.ticket.fast.ticket.config;


import org.apache.kafka.common.TopicPartition;
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
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<Object, Object> kafkaTemplate) {
        //기존토빅.DLQ 로 보내도록
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (cr, e) -> new TopicPartition(cr.topic() + ".DLQ", cr.partition())
        );
    }
    @Bean
    public DefaultErrorHandler errorHandler(DeadLetterPublishingRecoverer recoverer) {
        // 3번 실패 시 DLQ로 전송
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2);

        return new DefaultErrorHandler(recoverer, fixedBackOff);
    }


    // 배치 리스너 활성화
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler
    ) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(errorHandler);

        factory.setBatchListener(true);
        // JSON 문자열을 객체로 변환해 주는 배치 컨버터
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter(new StringJacksonJsonMessageConverter()));

        return factory;
    }
}
