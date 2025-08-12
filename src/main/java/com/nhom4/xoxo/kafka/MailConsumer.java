package com.nhom4.xoxo.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.dto.req.MailMessage;

import jakarta.mail.internet.InternetAddress;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailConsumer {
    private final JavaMailSender mailSender;

    
    public MailConsumer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @KafkaListener(topics = "${mail.topic}", groupId = "mail-group")
    public void listen(MailMessage message) {
        try {
            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, "UTF-8");
                messageHelper.setTo(message.getTo());
                messageHelper.setSubject(message.getSubject());
                messageHelper.setText(message.getContent(), true); // true = HTML content
                messageHelper
                        .setFrom(new InternetAddress("javamailsendertest6@gmail.com", "XOXO Social Media", "UTF-8"));
            };

            mailSender.send(messagePreparator);
            log.info("[MailConsumer] Sent email to {}", message.getTo());
        } catch (Exception e) {
            log.error("[MailConsumer] Mail send failed: {}", e.getMessage(), e);
            // Ném lại exception để DefaultErrorHandler xử lý retry/DLT
            throw new RuntimeException("Mail send failed: " + e.getMessage(), e);
        }
    }
}