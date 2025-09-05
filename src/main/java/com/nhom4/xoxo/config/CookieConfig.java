package com.nhom4.xoxo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieConfig {
    
    @Value("${app.is-production:false}")
    private boolean isProduction;
    
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;
    
    /**
     * Tạo refresh token cookie với cấu hình tối ưu cho HTTPS
     */
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true) // Luôn TRUE vì cả local và prod đều dùng HTTPS
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("None"); // An toàn vì đã có secure=true
        
        // Chỉ set domain cho production
        if (isProduction) {
            cookieBuilder.domain(".xoxo.id.vn");
        }
        
        return cookieBuilder.build();
    }
    
    /**
     * Tạo cookie để clear refresh token (logout)
     */
    public ResponseCookie createClearRefreshTokenCookie() {
        ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("None");
        
        if (isProduction) {
            cookieBuilder.domain(".xoxo.id.vn");
        }
        
        return cookieBuilder.build();
    }
}