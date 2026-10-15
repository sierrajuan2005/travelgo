package com.travelgo.Repository;

import com.travelgo.model.PaqueteTuristico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a datos de PaqueteTuristico mediante Spring Data JPA. */
public interface PaqueteRepository extends JpaRepository<PaqueteTuristico, Long> {

    List<PaqueteTuristico> findByActivoTrue();
}