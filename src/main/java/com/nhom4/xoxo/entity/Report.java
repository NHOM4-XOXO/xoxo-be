package com.nhom4.xoxo.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Report extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_target_type", nullable = false) // ✅ Đúng tên column
    private ReportTargetType reportTargetType;
    
    @Column(name = "report_target_id", nullable = false) // ✅ Đúng tên column
    private Long reportTargetId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false)
    private ReportReason reportReason;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false) // ✅ Đúng tên column
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist
    public void PrePersist() {
        if (status == null) {
            status = ReportStatus.PENDING;
        }
        if (isAnonymous == null) {
            isAnonymous = false;
        }
        if (priority == null) {
            priority = 1;
        }
    }
}
