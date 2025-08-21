package com.nhom4.xoxo.entity;

import java.time.LocalDateTime;

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
@Table(name = "user_devices")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDevice extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String deviceId; // Unique device identifier
    
    @Column(nullable = false)
    private String fcmToken; // Firebase Cloud Messaging token
    
    @Column(nullable = false)
    private String deviceType; // ANDROID, IOS, WEB
    
    @Column(nullable = false)
    private String deviceModel;
    
    @Column(nullable = false)
    private String operatingSystem;
    
    @Column(nullable = false)
    private String appVersion;
    
    @Column(nullable = false)
    private LocalDateTime lastSeenAt;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @Column(nullable = false)
    private boolean pushEnabled = true;
    
    @Column(nullable = false)
    private boolean chatNotifications = true;
    
    @Column(nullable = false)
    private boolean friendRequestNotifications = true;
    
    @Column(nullable = false)
    private boolean systemNotifications = true;
}
