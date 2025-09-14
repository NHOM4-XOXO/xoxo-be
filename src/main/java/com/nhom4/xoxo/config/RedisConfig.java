package com.nhom4.xoxo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {

    /**
     * Primary RedisTemplate bean for general use (String, Object)
     * This bean is required by MessengerChatServiceImpl
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Configure JSON serialization using the newer GenericJackson2JsonRedisSerializer
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

            // String serializer
            StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

            // Set key serializer to String
            template.setKeySerializer(stringRedisSerializer);
            template.setHashKeySerializer(stringRedisSerializer);

            // Set value serializer to JSON
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);

            // Enable transaction support
            template.setEnableTransactionSupport(true);
            template.afterPropertiesSet();

            log.info("RedisTemplate<String, Object> configured successfully");
            return template;

        } catch (Exception e) {
            log.error("Failed to configure RedisTemplate: {}", e.getMessage(), e);
            throw new RuntimeException("Redis configuration failed", e);
        }
    }

    /**
     * RedisTemplate for typing indicators - uses Jackson2JsonRedisSerializer
     * This avoids type information issues for simple Map objects
     */
    @Bean("typingRedisTemplate")
    public RedisTemplate<String, Object> typingRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // Configure JSON serialization using Jackson2JsonRedisSerializer
            Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

            // String serializer
            StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

            // Set key serializer to String
            template.setKeySerializer(stringRedisSerializer);
            template.setHashKeySerializer(stringRedisSerializer);

            // Set value serializer to JSON
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);

            // Enable transaction support
            template.setEnableTransactionSupport(true);
            template.afterPropertiesSet();

            log.info("TypingRedisTemplate configured successfully");
            return template;

        } catch (Exception e) {
            log.error("Failed to configure TypingRedisTemplate: {}", e.getMessage(), e);
            throw new RuntimeException("TypingRedisTemplate configuration failed", e);
        }
    }

    /**
     * StringRedisTemplate bean for string operations
     * Used by RefreshTokenServiceImpl and other string-based operations
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            StringRedisTemplate template = new StringRedisTemplate();
            template.setConnectionFactory(connectionFactory);
            template.setEnableTransactionSupport(true);
            template.afterPropertiesSet();

            log.info("StringRedisTemplate configured successfully");
            return template;

        } catch (Exception e) {
            log.error("Failed to configure StringRedisTemplate: {}", e.getMessage(), e);
            throw new RuntimeException("StringRedisTemplate configuration failed", e);
        }
    }

    /**
     * RedisTemplate for String-String operations
     * Used by RateLimitingFilter
     */
    @Bean("stringStringRedisTemplate")
    public RedisTemplate<String, String> stringStringRedisTemplate(RedisConnectionFactory connectionFactory) {
        try {
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(stringSerializer);

            template.setEnableTransactionSupport(true);
            template.afterPropertiesSet();

            log.info("RedisTemplate<String, String> configured successfully");
            return template;

        } catch (Exception e) {
            log.error("Failed to configure RedisTemplate<String, String>: {}", e.getMessage(), e);
            throw new RuntimeException("String-String RedisTemplate configuration failed", e);
        }
    }
}