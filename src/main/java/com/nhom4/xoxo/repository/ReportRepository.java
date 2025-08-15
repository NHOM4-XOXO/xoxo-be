package com.nhom4.xoxo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nhom4.xoxo.entity.Report;
import com.nhom4.xoxo.enums.ReportStatus;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(ReportStatus status);

}
