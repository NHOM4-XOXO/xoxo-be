package com.nhom4.xoxo.kafka;

import com.nhom4.xoxo.dto.req.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {
    
    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
    
    @Value("${notification.topic:notifications}")
    private String topic;
    
    public void sendNotification(NotificationMessage message) {
        try {
            CompletableFuture<SendResult<String, NotificationMessage>> future = 
                kafkaTemplate.send(topic, message.getUserId().toString(), message);
            
            SendResult<String, NotificationMessage> result = future.get(10, TimeUnit.SECONDS);
            
            if (result.getProducerRecord() != null) {
                log.info("[NotificationProducer] Sent notification to topic {} for user {}", 
                    topic, message.getUserId());
            }
        } catch (Exception e) {
            log.error("[NotificationProducer] Error sending notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send notification via Kafka", e);
        }
    }
}