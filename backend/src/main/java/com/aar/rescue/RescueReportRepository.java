package com.aar.rescue;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescueReportRepository extends JpaRepository<RescueReport, Long> {

    boolean existsByCode(String code);

    long countByReporterPhoneAndCreatedAtAfter(String phone, OffsetDateTime after);

    Page<RescueReport> findByStatus(ReportStatus status, Pageable pageable);
}
