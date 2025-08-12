package com.nhom4.xoxo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.nhom4.xoxo.dto.req.MailMessage;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.kafka.MailProducer;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import com.nhom4.xoxo.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    
    private final MailProducer mailProducer;
    private final VerificationTokenRepository verificationTokenRepository;
    private final TransactionTemplate transactionTemplate;
    
    public EmailServiceImpl(MailProducer mailProducer, 
                          VerificationTokenRepository verificationTokenRepository,
                          TransactionTemplate transactionTemplate) {
        this.mailProducer = mailProducer;
        this.verificationTokenRepository = verificationTokenRepository;
        this.transactionTemplate = transactionTemplate;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMailWithRollback(MailMessage mailMessage, VerificationToken verificationToken) {
        try {
            // Gửi mail
            mailProducer.sendMail(mailMessage);
            
            // Nếu gửi thành công, commit transaction
            log.info("[EmailService] Sent email to {}", mailMessage.getTo());
            
        } catch (Exception e) {
            // Nếu gửi mail thất bại, rollback và xóa token
            log.error("[EmailService] Error sending mail: {}", e.getMessage(), e);
            
            if (verificationToken != null) {
                try {
                    verificationTokenRepository.delete(verificationToken);
                    log.info("[EmailService] Deleted token after send failure");
                } catch (Exception deleteException) {
                    log.error("[EmailService] Error deleting token: {}", deleteException.getMessage(), deleteException);
                }
            }
            
            throw new ServiceException("Không thể gửi email. Vui lòng thử lại sau: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendMail(MailMessage mailMessage) {
        try {
            mailProducer.sendMail(mailMessage);
            log.info("[EmailService] Sent email to {}", mailMessage.getTo());
        } catch (Exception e) {
            log.error("[EmailService] Error sending mail: {}", e.getMessage(), e);
            throw new ServiceException("Không thể gửi email: " + e.getMessage());
        }
    }
} 