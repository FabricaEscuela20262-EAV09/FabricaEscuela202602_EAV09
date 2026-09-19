package com.reservar.repository;

import com.reservar.entity.Token;
import com.reservar.entity.enums.TipoToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    Optional<Token> findByHashTokenAndTipo(
            String hashToken,
            TipoToken tipo
    );

    Optional<Token> findByHashTokenAndTipoAndFechaUsoIsNull(
            String hashToken,
            TipoToken tipo
    );

    long deleteByUsuarioIdUsuario(UUID idUsuario);

    long countByUsuarioIdUsuarioAndTipoAndFechaUsoIsNullAndFechaExpiracionAfter(
            UUID idUsuario,
            TipoToken tipo,
            LocalDateTime fechaActual
    );

    long deleteByUsuarioIdUsuarioAndTipo(
            UUID idUsuario,
            TipoToken tipo
    );

    List<Token> findByTipoAndFechaUsoIsNullAndFechaExpiracionAfter(
            TipoToken tipo,
            LocalDateTime fechaActual
    );
}

