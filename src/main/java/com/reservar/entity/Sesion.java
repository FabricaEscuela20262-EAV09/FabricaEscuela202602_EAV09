package com.reservar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "\"SESION\"")
@Getter
@Setter
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_sesion")
    private UUID idSesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_sesion_usuario"))
    private Usuario usuario;

    @Column(name = "hash_token", nullable = false, length = 200)
    private String hashToken;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(name = "ip_origen", columnDefinition = "inet")
    @ColumnTransformer(write = "?::inet")
    private String ipOrigen;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}

