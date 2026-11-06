package com.reservar.repository;

import com.reservar.entity.Reserva;
import com.reservar.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    @Query("""
        SELECT COUNT(r) > 0
        FROM Reserva r
        WHERE r.servicio.idServicio = :idServicio
          AND r.estado IN :estados
          AND r.fechaInicio < :fechaFin
          AND r.fechaFin > :fechaInicio
        """)
    boolean existeReservaSuperpuesta(
            @Param("idServicio") Long idServicio,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            @Param("estados") List<EstadoReserva> estados
    );

    @Query("""
    SELECT r FROM Reserva r
    WHERE r.servicio.idServicio = :idServicio
      AND r.estado IN :estados
      AND r.fechaInicio < :finRango
      AND r.fechaFin > :inicioRango
    """)
    List<Reserva> findReservasActivasEnRango(
            @Param("idServicio") Long idServicio,
            @Param("estados") List<EstadoReserva> estados,
            @Param("inicioRango") LocalDateTime inicioRango,
            @Param("finRango") LocalDateTime finRango
    );
}