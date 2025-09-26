package com.nhom4.xoxo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            // Try to create Redis cache manager
            RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()));

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // ==================== User & Auth Caches ====================
            // User cache - longer TTL for user data
            cacheConfigurations.put("users", defaultCacheConfig.entryTtl(Duration.ofHours(1)));

            // User sessions and auth data
            cacheConfigurations.put("user-sessions", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
            cacheConfigurations.put("user-permissions", defaultCacheConfig.entryTtl(Duration.ofMinutes(45)));

            // ==================== Social Content Caches ====================
            // Group cache - medium TTL
            cacheConfigurations.put("groups", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

            // Post cache - medium TTL for post content
            cacheConfigurations.put("posts", defaultCacheConfig.entryTtl(Duration.ofMinutes(20)));

            // Post reactions - shorter TTL for dynamic data
            cacheConfigurations.put("post-reactions", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

            // Chat cache - very short TTL for real-time data
            cacheConfigurations.put("chat-rooms", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
            cacheConfigurations.put("chat-messages", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)));

            // ==================== Admin & Moderation Caches ====================
            // Report cache - shorter TTL for dynamic data
            cacheConfigurations.put("reports", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

            // Admin statistics - very short TTL
            cacheConfigurations.put("admin-stats", defaultCacheConfig.entryTtl(Duration.ofMinutes(3)));

            // ==================== Analytics Caches ====================
            // Analytics cache - very short TTL
            cacheConfigurations.put("analytics", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

            // Group analytics
            cacheConfigurations.put("group-analytics", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

            // Chat analytics
            cacheConfigurations.put("chat-analytics", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));

            // ==================== Discovery & Recommendation Caches ====================
            // Popular content cache
            cacheConfigurations.put("popular", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));

            // Suggested content cache
            cacheConfigurations.put("suggested", defaultCacheConfig.entryTtl(Duration.ofMinutes(20)));

            // Trending content
            cacheConfigurations.put("trending", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

            // Search results cache
            cacheConfigurations.put("search-results", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

            // ==================== Media & File Caches ====================
            // Media metadata cache
            cacheConfigurations.put("media", defaultCacheConfig.entryTtl(Duration.ofHours(2)));

            // File upload cache
            cacheConfigurations.put("file-uploads", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));

            // ==================== Notification Caches ====================
            // Notification cache
            cacheConfigurations.put("notifications", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

            // Unread counts cache
            cacheConfigurations.put("unread-counts", defaultCacheConfig.entryTtl(Duration.ofMinutes(2)));

            // ==================== Performance Caches ====================
            // Database query cache
            cacheConfigurations.put("db-queries", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));

            // API response cache
            cacheConfigurations.put("api-responses", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

            // ==================== NewsFeed Caches ====================
            // News feed cache - medium TTL for feed content
            cacheConfigurations.put("news-feed", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
            
            // News feed items cache - shorter TTL for real-time updates
            cacheConfigurations.put("news-feed-items", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
            
            // News feed analytics cache
            cacheConfigurations.put("news-feed-analytics", defaultCacheConfig.entryTtl(Duration.ofMinutes(60)));

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(defaultCacheConfig)
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .build();

        } catch (Exception e) {
            // Fallback to in-memory cache if Redis is not available
            return new ConcurrentMapCacheManager(
                    "users", "user-sessions", "user-permissions",
                    "groups", "posts", "post-reactions",
                    "chat-rooms", "chat-messages",
                    "reports", "admin-stats",
                    "analytics", "group-analytics", "chat-analytics",
                    "popular", "suggested", "trending", "search-results",
                    "media", "file-uploads",
                    "notifications", "unread-counts",
                    "db-queries", "api-responses",
                    "news-feed", "news-feed-items", "news-feed-analytics");
        }
    }

    // ==================== Cache Eviction Utilities ====================

    // Custom cache key generator for complex keys
    @org.springframework.context.annotation.Bean
    public org.springframework.cache.interceptor.KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(target.getClass().getSimpleName()).append(".");
            keyBuilder.append(method.getName());

            for (Object param : params) {
                keyBuilder.append(".").append(param != null ? param.toString() : "null");
            }

            return keyBuilder.toString();
        };
    }

    // Cache statistics bean for monitoring
    @org.springframework.context.annotation.Bean
    public CacheStatisticsService cacheStatisticsService(CacheManager cacheManager) {
        return new CacheStatisticsService(cacheManager);
    }

    // Cache statistics service
    public static class CacheStatisticsService {
        private final CacheManager cacheManager;

        public CacheStatisticsService(CacheManager cacheManager) {
            this.cacheManager = cacheManager;
        }

        public Map<String, Object> getCacheStatistics() {
            Map<String, Object> stats = new HashMap<>();

            cacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // Basic cache info
                    stats.put(cacheName + "_exists", true);

                    // For Redis cache, we could get more detailed stats
                    if (cache instanceof org.springframework.data.redis.cache.RedisCache) {
                        // Redis-specific statistics would go here
                        stats.put(cacheName + "_type", "Redis");
                    } else {
                        stats.put(cacheName + "_type", "InMemory");
                    }
                }
            });

            return stats;
        }

        public void evictAllCaches() {
            cacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }

        public void evictCache(String cacheName) {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
    }
