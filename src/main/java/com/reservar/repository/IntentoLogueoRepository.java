package com.reservar.repository;

import com.reservar.entity.IntentoLogueo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IntentoLogueoRepository extends JpaRepository<IntentoLogueo, Long> {

    long countByUsuarioIdUsuarioAndExitoso(
            UUID idUsuario,
            Boolean exitoso
    );
}
