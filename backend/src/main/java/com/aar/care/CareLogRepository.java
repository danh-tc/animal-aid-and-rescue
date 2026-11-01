package com.aar.care;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareLogRepository extends JpaRepository<CareLog, Long> {

    Page<CareLog> findByPetId(Long petId, Pageable pageable);
}
