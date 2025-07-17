package com.nhom4.xoxo.service;

import com.nhom4.xoxo.dto.MailMessage;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;

@Service
public class MailConsumer {
    private final JavaMailSender mailSender;

    @Autowired
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
                messageHelper.setFrom(new InternetAddress("javamailsendertest6@gmail.com", "XOXO Social Media", "UTF-8"));
            };
            
            mailSender.send(messagePreparator);
            System.out.println("[MailConsumer] Đã gửi mail thành công tới: " + message.getTo());
        } catch (Exception e) {
            System.err.println("[MailConsumer] Lỗi gửi mail: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 