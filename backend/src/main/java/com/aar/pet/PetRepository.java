package com.aar.pet;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetRepository extends JpaRepository<Pet, Long> {

    java.util.Optional<Pet> findByCode(String code);

    Page<Pet> findByStatus(PetStatus status, Pageable pageable);

    Page<Pet> findByStatusAndSpecies(PetStatus status, Species species, Pageable pageable);

    Optional<Pet> findByIdAndStatus(Long id, PetStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id")
    Optional<Pet> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        select p from Pet p
        where (:status is null or p.status = :status)
          and (:species is null or p.species = :species)
          and (lower(p.code) like :pattern or lower(coalesce(p.name, '')) like :pattern)
        """)
    Page<Pet> search(@Param("status") PetStatus status, @Param("species") Species species,
                     @Param("pattern") String pattern, Pageable pageable);
}
