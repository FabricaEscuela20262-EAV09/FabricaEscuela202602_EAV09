package com.reservar.repository;

import com.reservar.entity.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TipoServicioRepository
        extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findByCategoriaIdCategoriaAndEstado(
            Long idCategoria,
            String estado
    );

    List<TipoServicio> findByEstado(String estado);
}