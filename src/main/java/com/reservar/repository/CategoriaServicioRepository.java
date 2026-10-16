package com.reservar.repository;

import com.reservar.entity.CategoriaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface CategoriaServicioRepository
        extends JpaRepository<CategoriaServicio, Long> {

    Optional<CategoriaServicio> findByNombreAndEstado(
            String nombre,
            String estado
    );

    List<CategoriaServicio> findByEstado(String estado);
}