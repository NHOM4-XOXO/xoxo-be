package com.nhom4.xoxo.service;

import java.util.concurrent.TimeUnit;

public interface RefreshTokenService {
    void saveRefreshToken(String userEmail, String refreshToken, long duration, TimeUnit unit);
    void deleteRefreshToken(String userEmail);
    String getUserEmailFromRefreshToken(String refreshToken);
}