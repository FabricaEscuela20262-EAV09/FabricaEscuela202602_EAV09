package com.reservar.repository;

import com.reservar.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    List<Sesion> findByUsuarioIdUsuarioAndFechaRevocacionIsNull(
            UUID idUsuario
    );

    List<Sesion> findByUsuarioIdUsuarioAndFechaRevocacionIsNullAndFechaExpiracionAfter(
            UUID idUsuario,
            LocalDateTime fechaActual
    );

    @Query("""
            SELECT s FROM Sesion s
            JOIN FETCH s.usuario
            WHERE s.hashToken = :hashToken
            AND s.fechaRevocacion IS NULL
            AND s.fechaExpiracion > :fechaActual
            """)
    Optional<Sesion> findByHashTokenAndFechaRevocacionIsNullAndFechaExpiracionAfter(
            @Param("hashToken") String hashToken,
            @Param("fechaActual") LocalDateTime fechaActual
    );

    void deleteByUsuarioIdUsuario(UUID idUsuario);
}