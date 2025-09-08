package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom4.xoxo.entity.Report;
import com.nhom4.xoxo.entity.User;
import com.nhom4.xoxo.enums.ReportReason;
import com.nhom4.xoxo.enums.ReportStatus;
import com.nhom4.xoxo.enums.ReportTargetType;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // Basic queries
    List<Report> findByStatus(ReportStatus status);
    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    
    // Enhanced queries
    Page<Report> findByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);
    Page<Report> findByReportTargetTypeAndReportTargetIdOrderByCreatedAtDesc(
        ReportTargetType targetType, Long targetId, Pageable pageable);
    Page<Report> findByReportReasonOrderByCreatedAtDesc(ReportReason reason, Pageable pageable);
    Page<Report> findByPriorityOrderByCreatedAtDesc(Integer priority, Pageable pageable);
    
    // Analytics queries
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    long countByStatus(@Param("status") ReportStatus status);
    
    @Query("SELECT r.reportTargetType, COUNT(r) FROM Report r GROUP BY r.reportTargetType")
    List<Object[]> countByTargetType();
    
    @Query("SELECT r.reportReason, COUNT(r) FROM Report r GROUP BY r.reportReason")
    List<Object[]> countByReason();
    
    @Query("SELECT r.priority, COUNT(r) FROM Report r GROUP BY r.priority")
    List<Object[]> countByPriority();
    
    @Query("SELECT COUNT(DISTINCT r.reporter.id) FROM Report r")
    long countDistinctReporters();
    
    @Query("SELECT DATE(r.createdAt), COUNT(r) FROM Report r " +
           "WHERE r.createdAt >= :startDate " +
           "GROUP BY DATE(r.createdAt) " +
           "ORDER BY DATE(r.createdAt)")
    List<Object[]> countReportsThisWeek(@Param("startDate") java.time.LocalDateTime startDate);
    
    @Query("SELECT AVG(TIMESTAMPDIFF(HOUR, r.createdAt, r.reviewedAt)) FROM Report r " +
           "WHERE r.reviewedAt IS NOT NULL")
    Double getAverageResolutionTimeInHours();
    
    // High priority pending reports
    @Query("SELECT r FROM Report r WHERE r.status = com.nhom4.xoxo.enums.ReportStatus.PENDING AND r.priority >= 3 " +
           "ORDER BY r.priority DESC, r.createdAt ASC")
    List<Report> findHighPriorityPendingReports();
    
    // Reports by date range
    @Query("SELECT r FROM Report r WHERE r.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY r.createdAt DESC")
    Page<Report> findByCreatedAtBetween(
        @Param("startDate") java.time.LocalDateTime startDate,
        @Param("endDate") java.time.LocalDateTime endDate,
        Pageable pageable);
}
