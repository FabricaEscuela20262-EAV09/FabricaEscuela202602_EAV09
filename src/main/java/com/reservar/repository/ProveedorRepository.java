package com.reservar.repository;

import com.reservar.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByUsuarioIdUsuario(UUID idUsuario);

    Optional<Proveedor> findByCodigoProveedorIdCodigoProveedor(
            Long idCodigoProveedor
    );

    boolean existsByUsuarioIdUsuario(UUID idUsuario);
}

