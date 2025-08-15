// package com.nhom4.xoxo.notification;

// import java.time.Instant;

// import org.springframework.data.annotation.Id;
// import org.springframework.data.mongodb.core.mapping.Document;

// @Document(collection = "notifications")
// public class Notification {
//     @Id
//     private String id;
//     private Long userId; // recipient
//     private String type; // FRIEND_REQUEST, COMMENT, LIKE
//     private String payload; // JSON string payload
//     private boolean read;
//     private Instant createdAt = Instant.now();

//     public String getId() { return id; }
//     public void setId(String id) { this.id = id; }
//     public Long getUserId() { return userId; }
//     public void setUserId(Long userId) { this.userId = userId; }
//     public String getType() { return type; }
//     public void setType(String type) { this.type = type; }
//     public String getPayload() { return payload; }
//     public void setPayload(String payload) { this.payload = payload; }
//     public boolean isRead() { return read; }
//     public void setRead(boolean read) { this.read = read; }
//     public Instant getCreatedAt() { return createdAt; }
//     public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
// }



