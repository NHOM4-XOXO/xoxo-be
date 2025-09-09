package com.nhom4.xoxo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_reactions")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageReaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "reaction", nullable = false, length = 10)
    private String reaction; // 👍, ❤️, 😂, 😮, 😢, 😡, etc.
    
    // Unique constraint để user chỉ có thể react 1 lần cho 1 message
    @Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id", "user_id"})
    })
    public static class MessageReactionConstraint {}
}




