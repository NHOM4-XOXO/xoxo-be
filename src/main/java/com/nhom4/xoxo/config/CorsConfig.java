package com.nhom4.xoxo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Chỉ định rõ origins khi dùng allowCredentials=true
<<<<<<< HEAD
        configuration.setAllowedOrigins(List.of(
            "https://admin.xoxo.id.vn",
            "https://web.xoxo.id.vn",
            "https://localhost:3000",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));
=======
        configuration.setAllowedOrigins(List.of("https://admin.xoxo.id.vn", "https://localhost:3000","https://web.xoxo.id.vn","https://localhost:3001" ));
>>>>>>> 0a67354ea654c0834c8ffe5adb83421c514d8515
        
        // Cho phép các HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Cho phép tất cả headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Cho phép credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Thời gian cache preflight requests (1 giờ)
        configuration.setMaxAge(3600L);
        
        // Expose headers cho frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
} 