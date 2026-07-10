package com.aifms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Reactive configuration.
 * Provides a ReactiveRedisTemplate for reactive Redis operations.
 */
@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        var serializationContext = RedisSerializationContext
                .<String, Object>newSerializationContext(StringRedisSerializer.UTF_8)
                .value(RedisSerializationContext.SerializationPair.fromSerializer(
                        new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer()))
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
