package com.ticket.fast.ticket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory){

        // Key를 읽기 편하게 일반 문자열로 직렬화
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        // Value 를 Json 형식으로 직렬화
        JacksonJsonRedisSerializer<Object> valueSerializer = new JacksonJsonRedisSerializer<>(Object.class);

        RedisSerializationContext<String, Object> context = RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);

    }
}
