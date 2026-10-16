package com.reservar.repository;

import com.reservar.entity.DisponibilidadServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadServicioRepository
        extends JpaRepository<DisponibilidadServicio, Long> {

    List<DisponibilidadServicio> findByServicioIdServicio(
            Long idServicio
    );

    List<DisponibilidadServicio> findByServicioIdServicioAndDiaSemana(
            Long idServicio,
            String diaSemana
    );

    void deleteByServicioIdServicio(Long idServicio);
}