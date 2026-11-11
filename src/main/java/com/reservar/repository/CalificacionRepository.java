package com.reservar.repository;

import com.reservar.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {
    Optional<Calificacion> findByReservaIdReserva(Long idReserva);

    interface Resumen {
        Double getPromedio();
        Long getCantidad();
    }

    @Query("""
        SELECT AVG(c.estrellas) AS promedio, COUNT(c) AS cantidad
        FROM Calificacion c WHERE c.reserva.servicio.idServicio = :idServicio
        """)
    Resumen resumirPorServicio(@Param("idServicio") Long idServicio);
}
