package com.nhom4.xoxo.service;

import java.util.concurrent.TimeUnit;

public interface RefreshTokenService {
    void saveRefreshToken(String userId, String refreshToken, long duration, TimeUnit unit);
    String getRefreshToken(String userId);
    void deleteRefreshToken(String userId);
}