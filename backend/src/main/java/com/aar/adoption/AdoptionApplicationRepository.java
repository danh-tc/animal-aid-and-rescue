package com.aar.adoption;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionApplicationRepository extends JpaRepository<AdoptionApplication, Long> {

    List<AdoptionApplication> findByPetIdAndStatusIn(Long petId, Collection<ApplicationStatus> statuses);

    List<AdoptionApplication> findByPetIdOrderBySubmittedAtDesc(Long petId);

    List<AdoptionApplication> findByApplicantIdOrderBySubmittedAtDesc(Long applicantId);

    long countByApplicantIdAndStatus(Long applicantId, ApplicationStatus status);

    boolean existsByPetIdAndApplicantIdAndStatus(Long petId, Long applicantId, ApplicationStatus status);

    @Query("""
        select a from AdoptionApplication a
        where (:status is null or a.status = :status) and (:petId is null or a.petId = :petId)
        """)
    Page<AdoptionApplication> search(@Param("status") ApplicationStatus status, @Param("petId") Long petId,
                                     Pageable pageable);

    /** Chỉ đọc pet_id, không nạp entity vào persistence context trước khi khóa dòng thú. */
    @Query("select a.petId from AdoptionApplication a where a.id = :id")
    Optional<Long> findPetIdById(@Param("id") Long id);
}
