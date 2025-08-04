package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.req.MailMessage;
import com.nhom4.xoxo.entity.VerificationToken;

public interface EmailService {
    /**
     * Gửi email với cơ chế rollback khi thất bại
     * @param mailMessage Thông tin email cần gửi
     * @param verificationToken Token cần xóa nếu gửi mail thất bại
     */
    void sendMailWithRollback(MailMessage mailMessage, VerificationToken verificationToken);
    
    /**
     * Gửi email đơn giản
     * @param mailMessage Thông tin email cần gửi
     */
    void sendMail(MailMessage mailMessage);
} 