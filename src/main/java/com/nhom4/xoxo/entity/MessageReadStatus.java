package com.nhom4.xoxo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_read_status")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageReadStatus extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "is_delivered", nullable = false)
    private boolean delivered = false;
    
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
    
    // Unique constraint để tránh duplicate
    @Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "user_id"})
    })
    public static class MessageReadStatusConstraint {}
}





