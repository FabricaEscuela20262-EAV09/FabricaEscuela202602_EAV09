package com.reservar.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"OFERTA_SERVICIO\"")
@Getter
@Setter
public class OfertaServicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_proveedor",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_oferta_proveedor")
    )
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_tipo_servicio",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_oferta_tipo_servicio")
    )
    private TipoServicio tipoServicio;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "modalidad", nullable = false, length = 20)
    private String modalidad;

    @Column(name = "ubicacion", length = 255)
    private String ubicacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;

        if (estado == null) {
            estado = "ACTIVO";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}

