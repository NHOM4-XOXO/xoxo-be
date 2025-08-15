package com.nhom4.xoxo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_encryption_keys")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEncryptionKey extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String privateKey;
    
    @Column(nullable = false)
    private String keyFingerprint;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @Column(nullable = false)
    private Long keyVersion = 1L;
}
