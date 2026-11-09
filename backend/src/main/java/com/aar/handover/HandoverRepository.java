package com.aar.handover;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HandoverRepository extends JpaRepository<Handover, Long> {

    Optional<Handover> findFirstByPetIdAndStatusInOrderByHandedOverAtDesc(Long petId, Collection<HandoverStatus> statuses);

    List<Handover> findByPetIdOrderByHandedOverAtDesc(Long petId);

    @Query("""
        select h from Handover h
        where h.applicationId in (select a.id from AdoptionApplication a where a.applicantId = :applicantId)
        order by h.handedOverAt desc
        """)
    List<Handover> findByApplicant(@Param("applicantId") Long applicantId);
}
