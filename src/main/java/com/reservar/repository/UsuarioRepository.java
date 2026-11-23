package com.reservar.repository;

import com.reservar.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository
        extends JpaRepository<Usuario, UUID> {

    // La columna es citext, pero Hibernate envía el parámetro como varchar y PostgreSQL resuelve
    // "citext = varchar" como "text = text", que sí distingue mayúsculas. LOWER en ambos lados garantiza
    // que el inicio de sesión (HU-02) y el control de correo único (HU-01) no dependan de las mayúsculas.
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.correo) = LOWER(:correo)")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE LOWER(u.correo) = LOWER(:correo)")
    boolean existsByCorreo(@Param("correo") String correo);
}
