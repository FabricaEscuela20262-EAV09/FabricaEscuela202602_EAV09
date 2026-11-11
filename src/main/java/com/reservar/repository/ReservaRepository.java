package com.reservar.repository;

import com.reservar.entity.Reserva;
import com.reservar.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.idReserva = :idReserva")
    Optional<Reserva> findByIdParaActualizar(@Param("idReserva") Long idReserva);

    @Query("""
        SELECT r FROM Reserva r
        WHERE (r.cliente.idUsuario = :idUsuario OR r.servicio.proveedor.usuario.idUsuario = :idUsuario)
          AND (:estado IS NULL OR r.estado = :estado)
        """)
    Page<Reserva> findHistorialPropio(@Param("idUsuario") UUID idUsuario,
                                     @Param("estado") EstadoReserva estado, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE Reserva r SET r.estado = :completada, r.fechaActualizacion = :ahora
        WHERE r.estado = :confirmada AND r.fechaFin <= :ahora
        """)
    int completarFinalizadas(@Param("confirmada") EstadoReserva confirmada,
                            @Param("completada") EstadoReserva completada,
                            @Param("ahora") LocalDateTime ahora);

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