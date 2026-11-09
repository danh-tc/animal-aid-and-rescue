package com.aar.system;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VetClinicRepository extends JpaRepository<VetClinic, Long> {

    List<VetClinic> findByActiveTrueOrderByName();

    List<VetClinic> findAllByOrderByName();
}
