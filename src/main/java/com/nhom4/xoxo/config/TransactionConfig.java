package com.nhom4.xoxo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public TransactionTemplate transactionTemplate(org.springframework.transaction.PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
} 