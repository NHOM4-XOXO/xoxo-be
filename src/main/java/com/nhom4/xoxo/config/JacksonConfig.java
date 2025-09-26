package com.nhom4.xoxo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule to handle LocalDateTime and other Java 8 time types
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (use ISO-8601 strings instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
