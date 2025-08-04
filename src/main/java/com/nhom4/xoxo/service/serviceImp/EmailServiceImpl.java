package com.nhom4.xoxo.service.serviceImp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.nhom4.xoxo.dto.req.MailMessage;
import com.nhom4.xoxo.entity.VerificationToken;
import com.nhom4.xoxo.exception.ServiceException;
import com.nhom4.xoxo.kafka.MailProducer;
import com.nhom4.xoxo.repository.VerificationTokenRepository;
import com.nhom4.xoxo.service.EmailService;

@Service
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
            System.out.println("[EmailService] Gửi mail thành công: " + mailMessage.getTo());
            
        } catch (Exception e) {
            // Nếu gửi mail thất bại, rollback và xóa token
            System.err.println("[EmailService] Lỗi gửi mail: " + e.getMessage());
            
            if (verificationToken != null) {
                try {
                    verificationTokenRepository.delete(verificationToken);
                    System.out.println("[EmailService] Đã xóa token sau khi gửi mail thất bại");
                } catch (Exception deleteException) {
                    System.err.println("[EmailService] Lỗi khi xóa token: " + deleteException.getMessage());
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
            System.out.println("[EmailService] Gửi mail thành công: " + mailMessage.getTo());
        } catch (Exception e) {
            System.err.println("[EmailService] Lỗi gửi mail: " + e.getMessage());
            throw new ServiceException("Không thể gửi email: " + e.getMessage());
        }
    }
} 