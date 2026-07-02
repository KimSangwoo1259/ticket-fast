package com.ticket.fast.ticket.config;


import com.ticket.fast.ticket.dto.event.ReservationEvent;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

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

    @Bean
    public ConsumerFactory<String, ReservationEvent> consumerFactory(
            KafkaProperties kafkaProperties
    ) {
        Map<String, Object> props =
                new HashMap<>(kafkaProperties.buildConsumerProperties());

        JacksonJsonDeserializer<ReservationEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(ReservationEvent.class);

        valueDeserializer.configure(Map.of(
                JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "com.ticket.fast.ticket.dto.event"
        ), false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> batchKafkaListenerContainerFactory(
            ConsumerFactory<String, ReservationEvent> consumerFactory,
            DefaultErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.setBatchListener(true);
        return factory;
    }

    // 🏷️ 2. DLQ 토픽용 팩토리 (단건 리스너)
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> singleKafkaListenerContainerFactory(
            ConsumerFactory<String, ReservationEvent> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, ReservationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);


        //DLQ 컨슈머에서 에러가 났을 때 또 .DLQ.DLQ로 가는 걸 막기 위해,
        // 백오프(재시도)만 설정해 줍니다.
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2)));

        return factory;
    }
}
