package com.nhom4.xoxo.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.nhom4.xoxo.dto.req.MailMessage;

@Service
public class MailProducer {
    private final KafkaTemplate<String, MailMessage> kafkaTemplate;
    private final String topic;

    @Autowired
    public MailProducer(KafkaTemplate<String, MailMessage> kafkaTemplate, @Value("${mail.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMail(MailMessage message) {
        kafkaTemplate.send(topic, message);
    }
} 