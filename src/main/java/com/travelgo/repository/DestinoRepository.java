package com.travelgo.repository;

import com.travelgo.model.Destino;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a datos de Destino mediante Spring Data JPA. */
public interface DestinoRepository extends JpaRepository<Destino, Long> {
}