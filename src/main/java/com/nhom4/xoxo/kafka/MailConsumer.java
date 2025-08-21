package com.nhom4.xoxo.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.nhom4.xoxo.dto.req.MailMessage;

import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailConsumer {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.from.name:XOXO Social Media}")
    private String fromName;

    public MailConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @KafkaListener(topics = "${mail.topic}", groupId = "mail-group")
    public void listen(MailMessage message) {
        log.info("[MailConsumer] Received message for email: {}", message.getTo());
        log.info("[MailConsumer] Message subject: {}", message.getSubject());
        log.info("[MailConsumer] Using sender email: {}", fromEmail);
        
        try {
            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");
                messageHelper.setTo(message.getTo());
                messageHelper.setSubject(message.getSubject());
                messageHelper.setText(message.getContent(), true); // true = HTML content
                messageHelper.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
            };
    
            log.info("[MailConsumer] Attempting to send email to: {}", message.getTo());
            mailSender.send(messagePreparator);
            log.info("[MailConsumer] Successfully sent email to: {}", message.getTo());
            
        } catch (Exception e) {
            log.error("[MailConsumer] Failed to send email to {}: {}", message.getTo(), e.getMessage(), e);
            log.error("[MailConsumer] Error details: {}", e.getClass().getSimpleName());
            
            // Log specific error details
            if (e.getMessage() != null) {
                log.error("[MailConsumer] Error message: {}", e.getMessage());
            }
            
            // Ném lại exception để DefaultErrorHandler xử lý retry/DLT
            throw new RuntimeException("Mail send failed for " + message.getTo() + ": " + e.getMessage(), e);
        }
    }
}