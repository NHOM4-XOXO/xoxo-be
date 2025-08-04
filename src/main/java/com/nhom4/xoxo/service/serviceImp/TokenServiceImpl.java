package com.nhom4.xoxo.service.serviceImp;

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

@Service
public class TokenServiceImpl implements TokenService {
    
    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    
    public TokenServiceImpl(UserRepository userRepository, VerificationTokenRepository verificationTokenRepository) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
    }
    
    @Override
    @Transactional
    public VerificationToken createForgotPasswordToken(String email) {
        System.out.println("[TokenService] === CREATE/REGENERATE TOKEN START ===");
        System.out.println("[TokenService] Email: " + email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        
        System.out.println("[TokenService] User found: " + user.getEmail());
        
        // Xóa các token cũ của user này (nếu có)
        deleteOldTokens(user, "FORGOT_PASSWORD");
        
        // Tạo token mới
        String token = UUID.randomUUID().toString();
        String type = "FORGOT_PASSWORD";
        VerificationToken verificationToken = new VerificationToken(token, user, LocalDateTime.now().plusHours(1), type);
        
        System.out.println("[TokenService] Tạo token mới: " + token);
        System.out.println("[TokenService] User ID: " + user.getId());
        
        try {
            VerificationToken savedToken = verificationTokenRepository.save(verificationToken);
            System.out.println("[TokenService] Token saved successfully with ID: " + savedToken.getId());
            System.out.println("[TokenService] === CREATE/REGENERATE TOKEN END ===");
            return savedToken;
        } catch (Exception e) {
            System.err.println("[TokenService] ERROR saving token: " + e.getMessage());
            System.err.println("[TokenService] Exception type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            throw new ServiceException("Không thể tạo token mới: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
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
    @Transactional
    public void deleteToken(VerificationToken token) {
        if (token != null) {
            verificationTokenRepository.delete(token);
        }
    }
    
    /**
     * Xóa các token cũ của user
     */
    private void deleteOldTokens(User user, String type) {
        System.out.println("[TokenService] Bắt đầu xóa token cũ cho user: " + user.getEmail() + ", type: " + type);
        
        try {
            List<VerificationToken> oldTokens = verificationTokenRepository.findByUserAndType(user, type);
            System.out.println("[TokenService] Tìm thấy " + oldTokens.size() + " token cũ");
            
            if (!oldTokens.isEmpty()) {
                // Log chi tiết các token sẽ xóa
                for (VerificationToken token : oldTokens) {
                    System.out.println("[TokenService] Sẽ xóa token ID: " + token.getId() + ", Token: " + token.getToken());
                }
                
                verificationTokenRepository.deleteAll(oldTokens);
                System.out.println("[TokenService] Đã xóa " + oldTokens.size() + " token cũ của user: " + user.getEmail());
                
                // Flush để đảm bảo delete được commit
                verificationTokenRepository.flush();
                System.out.println("[TokenService] Flush completed");
            } else {
                System.out.println("[TokenService] Không có token cũ để xóa");
            }
        } catch (Exception e) {
            System.err.println("[TokenService] ERROR khi xóa token cũ: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException("Không thể xóa token cũ: " + e.getMessage());
        }
    }
} 