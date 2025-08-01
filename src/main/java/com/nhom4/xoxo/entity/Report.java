package com.nhom4.xoxo.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Report extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "report_type", nullable = false)
    private Integer reportType;

    @Column(name = "report_reason", columnDefinition = "TEXT")
    private String reportReason;

    @Column(name = "status", columnDefinition = "TEXT")
    private String status;

    @Column(name = "report_at")
    private LocalDate reportAt;
}
