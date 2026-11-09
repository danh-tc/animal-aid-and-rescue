package com.aar.handover;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByHandoverIdOrderBySequenceNo(Long handoverId);

    List<FollowUp> findByHandoverIdAndStatus(Long handoverId, FollowUpStatus status);

    List<FollowUp> findByHandoverIdInOrderBySequenceNo(List<Long> handoverIds);

    List<FollowUp> findByStatusOrderByDueDateAscIdAsc(FollowUpStatus status);

    List<FollowUp> findByStatusAndDueDateLessThanEqualOrderByDueDateAscIdAsc(FollowUpStatus status, LocalDate dueBefore);
}
