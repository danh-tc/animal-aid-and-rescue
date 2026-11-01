package com.aar.pet;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetRepository extends JpaRepository<Pet, Long> {

    Page<Pet> findByStatus(PetStatus status, Pageable pageable);

    Page<Pet> findByStatusAndSpecies(PetStatus status, Species species, Pageable pageable);

    Optional<Pet> findByIdAndStatus(Long id, PetStatus status);
}
