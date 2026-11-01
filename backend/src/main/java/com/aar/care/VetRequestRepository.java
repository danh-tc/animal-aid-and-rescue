package com.aar.care;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VetRequestRepository extends JpaRepository<VetRequest, Long> {

    List<VetRequest> findByPetIdOrderByRequestedAtDesc(Long petId);

    long countByPetIdAndStatusIn(Long petId, Collection<VetRequestStatus> statuses);

    boolean existsByPetIdAndStatus(Long petId, VetRequestStatus status);

    Page<VetRequest> findByStatusIn(Collection<VetRequestStatus> statuses, Pageable pageable);
}
