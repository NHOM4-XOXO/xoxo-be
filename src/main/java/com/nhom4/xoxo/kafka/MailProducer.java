package com.nhom4.xoxo.kafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.dto.req.MailMessage;
import com.nhom4.xoxo.exception.ServiceException;

@Service
public class MailProducer {
    private final KafkaTemplate<String, MailMessage> kafkaTemplate;
    private final String topic;

    
    public MailProducer(KafkaTemplate<String, MailMessage> kafkaTemplate, @Value("${mail.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMail(MailMessage message) {
        try {
            CompletableFuture<SendResult<String, MailMessage>> future = kafkaTemplate.send(topic, message);
            
            // Chờ kết quả gửi với timeout 10 giây
            SendResult<String, MailMessage> result = future.get(10, TimeUnit.SECONDS);
            
            if (result.getProducerRecord() == null) {
                throw new ServiceException("Không thể gửi email: Kafka producer error");
            }
            
            System.out.println("[MailProducer] Đã gửi mail thành công tới Kafka topic: " + topic);
        } catch (Exception e) {
            System.err.println("[MailProducer] Lỗi gửi mail: " + e.getMessage());
            throw new ServiceException("Không thể gửi email qua Kafka: " + e.getMessage());
        }
    }
} 