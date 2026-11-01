package com.aar.pet;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetEndEventRepository extends JpaRepository<PetEndEvent, Long> {

    List<PetEndEvent> findByPetIdOrderByOccurredAtDesc(Long petId);
}
