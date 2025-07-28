package com.nhom4.xoxo.service.serviceImp;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.service.RefreshTokenService;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private final String PREFIX = "refresh_token:";

    // Lưu key là refreshToken, value là userEmail
    @Override
    public void saveRefreshToken(String refreshToken, String userEmail, long duration, TimeUnit unit) {
        redisTemplate.opsForValue().set(PREFIX + refreshToken, userEmail, duration, unit);
    }

    // Lấy userEmail từ refreshToken
    @Override
    public String getUserEmailFromRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(PREFIX + refreshToken);
    }

    // Xóa refreshToken
    @Override
    public void deleteRefreshToken(String refreshToken) {
        redisTemplate.delete(PREFIX + refreshToken);
    }

}
