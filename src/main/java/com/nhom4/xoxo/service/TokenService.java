package com.nhom4.xoxo.service;

import com.nhom4.xoxo.entity.VerificationToken;

public interface TokenService {
    /**
     * Tạo token mới cho forgot password
     * @param email Email của user
     * @return Token đã tạo
     */
    VerificationToken createForgotPasswordToken(String email);
    
    /**
     * Regenerate token cho forgot password (xóa token cũ, tạo token mới)
     * @param email Email của user
     * @return Token mới đã tạo
     */
    VerificationToken regenerateForgotPasswordToken(String email);
    
    /**
     * Validate token
     * @param token Token cần validate
     * @param type Loại token (FORGOT_PASSWORD, REGISTER)
     * @return Token nếu hợp lệ
     */
    VerificationToken validateToken(String token, String type);
    
    /**
     * Xóa token
     * @param token Token cần xóa
     */
    void deleteToken(VerificationToken token);
} 