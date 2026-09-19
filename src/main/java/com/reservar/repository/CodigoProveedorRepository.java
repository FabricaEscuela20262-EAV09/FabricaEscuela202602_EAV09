package com.reservar.repository;

import com.reservar.entity.CodigoProveedor;
import com.reservar.entity.enums.EstadoCodigoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CodigoProveedorRepository extends JpaRepository<CodigoProveedor, Long> {

    Optional<CodigoProveedor> findByCodigo(String codigo);

    Optional<CodigoProveedor> findByCodigoAndEstado(
            String codigo,
            EstadoCodigoProveedor estado
    );
}

