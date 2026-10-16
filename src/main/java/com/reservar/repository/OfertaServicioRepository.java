package com.reservar.repository;

import com.reservar.entity.OfertaServicio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OfertaServicioRepository
        extends JpaRepository<OfertaServicio, Long> {

    Page<OfertaServicio> findByEstado(
            String estado,
            Pageable pageable
    );

    Page<OfertaServicio> findByProveedorIdProveedorAndEstado(
            Long idProveedor,
            String estado,
            Pageable pageable
    );

    Page<OfertaServicio> findByProveedorIdProveedor(
            Long idProveedor,
            Pageable pageable
    );

    Optional<OfertaServicio> findByIdServicioAndEstado(
            Long idServicio,
            String estado
    );

    @Query("""
        SELECT o
        FROM OfertaServicio o JOIN o.tipoServicio t JOIN t.categoria c
        WHERE o.estado = 'ACTIVO' AND t.estado = 'ACTIVO' AND c.estado = 'ACTIVO'
          AND (
              :texto = ''
              OR LOWER(o.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
              OR LOWER(o.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
          )
          AND (
              :idCategoria IS NULL
              OR c.idCategoria = :idCategoria
          )
        """)
    Page<OfertaServicio> buscarOfertasActivas(
            @Param("texto") String texto,
            @Param("idCategoria") Long idCategoria,
            Pageable pageable
    );
}