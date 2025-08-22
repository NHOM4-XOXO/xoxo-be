package com.nhom4.xoxo.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.exception.NotFoundException;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.repository.UserRepository;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import com.nhom4.xoxo.service.TokenService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TokenServiceImpl implements TokenService {
    
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    
    public TokenServiceImpl(UserRepository userRepository, VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
    }
    
    @Override
    @Transactional(transactionManager = "transactionManager")
    public VerificationToken createForgotPasswordToken(String email) {
        log.info("[TokenService] === CREATE/REGENERATE TOKEN START ===");
        log.debug("[TokenService] Email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        log.debug("[TokenService] User found: {}", user.getEmail());
        
        // Xóa các token cũ của user này (nếu có)
        deleteOldTokens(user, "FORGOT_PASSWORD");
        
        // Tạo token mới
        String token = UUID.randomUUID().toString();
        String type = "FORGOT_PASSWORD";
        VerificationToken verificationToken = new VerificationToken(token, user, LocalDateTime.now().plusHours(1), type);
        
        log.debug("[TokenService] New token: {}", token);
        log.debug("[TokenService] User ID: {}", user.getId());
        
        try {
            VerificationToken savedToken = verificationTokenRepository.save(verificationToken);
            log.info("[TokenService] Token saved successfully with ID: {}", savedToken.getId());
            log.info("[TokenService] === CREATE/REGENERATE TOKEN END ===");
            return savedToken;
        } catch (Exception e) {
            log.error("[TokenService] ERROR saving token: {}", e.getMessage(), e);
            log.error("[TokenService] Exception type: {}", e.getClass().getSimpleName());
            throw new ServiceException("Không thể tạo token mới: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(transactionManager = "transactionManager")
    public VerificationToken regenerateForgotPasswordToken(String email) {
        // Gọi lại method createForgotPasswordToken vì logic giống hệt nhau
        return createForgotPasswordToken(email);
    }
    
    @Override
    public VerificationToken validateToken(String token, String type) {
        VerificationToken verificationToken = verificationTokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> new NotFoundException("Token not found or invalid"));
        
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Xóa token hết hạn
            verificationTokenRepository.delete(verificationToken);
            throw new ServiceException("Token has expired");
        }
        
        return verificationToken;
    }
    
    @Override
    public void deleteToken(VerificationToken token) {
        if (token != null) {
            verificationTokenRepository.delete(token);
        }
    }
    
    /**
     * Xóa các token cũ của user
     */
    private void deleteOldTokens(User user, String type) {
        log.info("[TokenService] Start deleting old tokens for user: {}, type: {}", user.getEmail(), type);
        
        try {
            List<VerificationToken> oldTokens = verificationTokenRepository.findByUserAndType(user, type);
            log.debug("[TokenService] Found {} old tokens", oldTokens.size());
            
            if (!oldTokens.isEmpty()) {
                // Log chi tiết các token sẽ xóa
                for (VerificationToken token : oldTokens) {
                    log.debug("[TokenService] Will delete token ID: {}, Token: {}", token.getId(), token.getToken());
                }
                
                verificationTokenRepository.deleteAll(oldTokens);
                log.info("[TokenService] Deleted {} old tokens for user: {}", oldTokens.size(), user.getEmail());
                
                // Flush để đảm bảo delete được commit
                verificationTokenRepository.flush();
                log.debug("[TokenService] Flush completed");
            } else {
                log.debug("[TokenService] No old token to delete");
            }
        } catch (Exception e) {
            log.error("[TokenService] ERROR deleting old tokens: {}", e.getMessage(), e);
            throw new ServiceException("Không thể xóa token cũ: " + e.getMessage());
        }
    }
} 