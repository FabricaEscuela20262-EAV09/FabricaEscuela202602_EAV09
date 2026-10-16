package com.reservar.repository;

import com.reservar.entity.BloqueoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloqueoDisponibilidadRepository
        extends JpaRepository<BloqueoDisponibilidad, Long> {

    List<BloqueoDisponibilidad> findByServicioIdServicioAndFecha(
            Long idServicio,
            LocalDate fecha
    );

    List<BloqueoDisponibilidad> findByServicioIdServicioAndFechaBetween(
            Long idServicio,
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    List<BloqueoDisponibilidad> findByServicioIdServicio(Long idServicio);

    void deleteByServicioIdServicio(Long idServicio);
}