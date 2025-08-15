package com.nhom4.xoxo.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MongoNotification {
    @Id
    private String id;
    
    @Indexed
    private Long userId; // Người nhận
    
    private String message;
    private String type;
    private Long targetId;
    private String targetType;
    private Long senderId;
    private String actionType;
    private String payload;
    private boolean read;
    
    @Indexed
    private Instant createdAt = Instant.now();
    
    private Instant readAt;
}