package com.aar.common.state;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedAtAsc(EntityType entityType, Long entityId);
}
