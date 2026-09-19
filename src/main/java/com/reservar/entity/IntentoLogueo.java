package com.reservar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"INTENTOS_LOGUEO\"")
@Getter
@Setter
public class IntentoLogueo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_intento")
    private Long idIntento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", foreignKey = @ForeignKey(name = "fk_intento_usuario"))
    private Usuario usuario;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso;

    @Column(name = "ip_origen", columnDefinition = "inet")
    @ColumnTransformer(write = "?::inet")
    private String ipOrigen;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento;

    @PrePersist
    protected void onCreate() {
        if (fechaIntento == null) {
            fechaIntento = LocalDateTime.now();
        }
    }
}

