package com.nhom4.xoxo.service.serviceImp;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.service.RefreshTokenService;

@Service
public class RedisRefreshTokenService implements RefreshTokenService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private final String PREFIX = "refresh_token:";

    @Override
    public void saveRefreshToken(String userId, String refreshToken, long duration, TimeUnit unit) {
        redisTemplate.opsForValue().set(PREFIX + userId, refreshToken, duration, unit);
    }

    @Override
    public String getRefreshToken(String userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }

    @Override
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}
