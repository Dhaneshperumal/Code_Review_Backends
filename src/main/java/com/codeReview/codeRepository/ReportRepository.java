package com.codeReview.codeRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codeReview.code.Report;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // You can add custom query methods here if needed
}