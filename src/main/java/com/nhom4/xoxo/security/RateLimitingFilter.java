package com.nhom4.xoxo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom4.xoxo.constant.WrapResStatus;
import com.nhom4.xoxo.dto.WrapRes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Fallback to in-memory cache if Redis is not available
    private final ConcurrentHashMap<String, AtomicInteger> inMemoryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> inMemoryTimestamps = new ConcurrentHashMap<>();
    
    // Rate limiting configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_ATTEMPTS_PER_HOUR = 50;
    private static final int MAX_REPORT_REQUESTS_PER_HOUR =50;
    private static final int MAX_SEARCH_REQUESTS_PER_MINUTE = 50;
    
    public RateLimitingFilter(@Qualifier("stringStringRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIpAddress(request);
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        
        // Apply different rate limits based on endpoint
        if (shouldRateLimit(requestPath, method)) {
            RateLimitConfig config = getRateLimitConfig(requestPath, method);
            
            if (!isRequestAllowed(clientIp, requestPath, config)) {
                sendRateLimitExceededResponse(response);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean shouldRateLimit(String path, String method) {
        // Rate limit sensitive endpoints
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/v1/reports") ||
               path.startsWith("/api/admin/") ||
               path.startsWith("/api/user/search");
    }
    
    private RateLimitConfig getRateLimitConfig(String path, String method) {
        if (path.startsWith("/api/auth/login")) {
            return new RateLimitConfig(MAX_LOGIN_ATTEMPTS_PER_HOUR, Duration.ofHours(1));
        } else if (path.startsWith("/api/v1/reports") && "POST".equals(method)) {
            return new RateLimitConfig(MAX_REPORT_REQUESTS_PER_HOUR, Duration.ofHours(1));
        } else if (path.startsWith("/api/user/search")) {
            return new RateLimitConfig(MAX_SEARCH_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        } else {
            return new RateLimitConfig(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        }
    }
    
    private boolean isRequestAllowed(String clientIp, String path, RateLimitConfig config) {
        String key = "rate_limit:" + clientIp + ":" + path;
        
        try {
            // Try Redis first
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            if (currentCount >= config.maxRequests) {
                return false;
            }
            
            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", config.duration);
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            
            return true;
            
        } catch (Exception e) {
            // Fallback to in-memory cache
            log.warn("Redis unavailable, using in-memory rate limiting for {}", clientIp);
            return isRequestAllowedInMemory(key, config);
        }
    }
    
    private boolean isRequestAllowedInMemory(String key, RateLimitConfig config) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - config.duration.toMillis();
        
        // Clean up old entries
        inMemoryTimestamps.entrySet().removeIf(entry -> entry.getValue() < windowStart);
        inMemoryCache.entrySet().removeIf(entry -> 
            !inMemoryTimestamps.containsKey(entry.getKey()));
        
        AtomicInteger counter = inMemoryCache.computeIfAbsent(key, k -> {
            inMemoryTimestamps.put(k, currentTime);
            return new AtomicInteger(0);
        });
        
        if (counter.get() >= config.maxRequests) {
            return false;
        }
        
        counter.incrementAndGet();
        return true;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private void sendRateLimitExceededResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        WrapRes<Object> errorResponse = WrapRes.error(
            WrapResStatus.TOO_MANY_REQUESTS, 
            "Quá nhiều yêu cầu. Vui lòng thử lại sau."
        );
        
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
    
    private static class RateLimitConfig {
        final int maxRequests;
        final Duration duration;
        
        RateLimitConfig(int maxRequests, Duration duration) {
            this.maxRequests = maxRequests;
            this.duration = duration;
        }
    }
}

