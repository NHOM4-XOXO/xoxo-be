package com.nhom4.xoxo.entity;

import java.time.LocalDateTime;

import com.nhom4.xoxo.enums.FileType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_files")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatFile extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String fileUrl;
    
    @Column(nullable = false)
    private String cloudinaryPublicId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private String mimeType;
    
    @Column(nullable = true)
    private String thumbnailUrl;
    
    @Column(nullable = true)
    private Integer duration; // For video/audio files in seconds
    
    @Column(nullable = true)
    private Integer width; // For image/video files
    
    @Column(nullable = true)
    private Integer height; // For image/video files
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id", nullable = false)
    private ChatMessage chatMessage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
    
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    
    @Column(nullable = false)
    private boolean encrypted = false;
    
    @Column(nullable = true)
    private String encryptionKey; // Encrypted AES key if file is encrypted
    
    @Column(nullable = false)
    private boolean active = true;
}
