package com.aar.pet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntakeRepository extends JpaRepository<Intake, Long> {

    List<Intake> findByPetIdOrderByIntakeAtDesc(Long petId);

    Optional<Intake> findFirstByPetIdOrderByIntakeAtDesc(Long petId);

    Optional<Intake> findByRescueCaseId(Long rescueCaseId);

    Optional<Intake> findByReturnedHandoverId(Long handoverId);
}
