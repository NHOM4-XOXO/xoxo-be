package com.nhom4.xoxo.controller;

import com.nhom4.xoxo.config.CacheConfig;
import com.nhom4.xoxo.dto.WrapRes;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@Tag(name = "Cache Management", description = "Cache management APIs for administrators")
public class CacheManagementController {

    private final CacheConfig.CacheStatisticsService cacheStatisticsService;
    private final CacheManager cacheManager;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userService.findByEmail(email);
    }

    @Operation(summary = "Get cache statistics", description = "Get detailed cache statistics and health")
    @GetMapping("/stats")
    public ResponseEntity<WrapRes<Map<String, Object>>> getCacheStatistics() {
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("403", "Admin or Owner role required"));
        }

        Map<String, Object> stats = cacheStatisticsService.getCacheStatistics();
        
        // Add additional cache info
        stats.put("totalCaches", cacheManager.getCacheNames().size());
        stats.put("cacheNames", cacheManager.getCacheNames());
        stats.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(WrapRes.success(stats));
    }

    @Operation(summary = "Clear all caches", description = "Clear all application caches")
    @DeleteMapping("/clear-all")
    public ResponseEntity<WrapRes<String>> clearAllCaches() {
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("403", "Admin or Owner role required"));
        }

        cacheStatisticsService.evictAllCaches();
        return ResponseEntity.ok(WrapRes.success("All caches cleared successfully"));
    }

    @Operation(summary = "Clear specific cache", description = "Clear a specific cache by name")
    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<WrapRes<String>> clearSpecificCache(
            @Parameter(description = "Name of cache to clear") @PathVariable String cacheName) {
        
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("403", "Admin or Owner role required"));
        }

        if (!cacheManager.getCacheNames().contains(cacheName)) {
            return ResponseEntity.badRequest()
                    .body(WrapRes.error("400", "Cache not found: " + cacheName));
        }

        cacheStatisticsService.evictCache(cacheName);
        return ResponseEntity.ok(WrapRes.success("Cache '" + cacheName + "' cleared successfully"));
    }

    @Operation(summary = "Get cache names", description = "Get list of all available cache names")
    @GetMapping("/names")
    public ResponseEntity<WrapRes<java.util.Collection<String>>> getCacheNames() {
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("403", "Admin or Owner role required"));
        }

        return ResponseEntity.ok(WrapRes.success(cacheManager.getCacheNames()));
    }

    @Operation(summary = "Warm up caches", description = "Pre-load important caches with data")
    @PostMapping("/warmup")
    public ResponseEntity<WrapRes<String>> warmupCaches() {
        User currentUser = getCurrentUser();
        if (!userService.isAdminOrOwner(currentUser)) {
            return ResponseEntity.status(403)
                    .body(WrapRes.error("403", "Admin or Owner role required"));
        }

        // Implement cache warmup logic
        // This could pre-load popular posts, trending groups, etc.
        
        return ResponseEntity.ok(WrapRes.success("Cache warmup initiated"));
    }
}





