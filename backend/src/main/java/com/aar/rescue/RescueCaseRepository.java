package com.aar.rescue;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    Page<RescueCase> findByStatus(CaseStatus status, Pageable pageable);

    @Query("select c from RescueCase c join c.reports r where r.id = :reportId order by c.id")
    List<RescueCase> findByReportId(@Param("reportId") Long reportId);
}
