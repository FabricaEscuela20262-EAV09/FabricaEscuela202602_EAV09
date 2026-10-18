package com.reservar.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Callbacks de ciclo de vida de las entidades de la Épica 2 (se invocan directamente: son métodos protegidos del paquete). */
class EntidadesServicioTest {

    @Test
    void deberiaAsignarFechasYEstadoActivoPorDefectoAlCrearUnaOferta() {
        OfertaServicio oferta = new OfertaServicio();

        oferta.onCreate();

        assertAll(
                () -> assertNotNull(oferta.getFechaCreacion()),
                () -> assertEquals(oferta.getFechaCreacion(), oferta.getFechaActualizacion()),
                () -> assertEquals("ACTIVO", oferta.getEstado())
        );
    }

    @Test
    void noDeberiaSobrescribirElEstadoDefinidoAlCrearUnaOferta() {
        OfertaServicio oferta = new OfertaServicio();
        oferta.setEstado("INACTIVO");

        oferta.onCreate();

        assertEquals("INACTIVO", oferta.getEstado());
    }

    @Test
    void deberiaRefrescarLaFechaDeActualizacionAlModificarUnaOferta() {
        OfertaServicio oferta = new OfertaServicio();
        LocalDateTime antigua = LocalDateTime.of(2020, 1, 1, 0, 0);
        oferta.setFechaActualizacion(antigua);

        oferta.onUpdate();

        assertTrue(oferta.getFechaActualizacion().isAfter(antigua));
    }

    @Test
    void deberiaUsarMotivoOtroPorDefectoAlPersistirUnBloqueo() {
        BloqueoDisponibilidad sinMotivo = new BloqueoDisponibilidad();
        BloqueoDisponibilidad conMotivo = new BloqueoDisponibilidad();
        conMotivo.setMotivo("RESERVADO");

        sinMotivo.onCreate();
        conMotivo.onCreate();

        assertAll(
                () -> assertEquals("OTRO", sinMotivo.getMotivo()),
                () -> assertEquals("RESERVADO", conMotivo.getMotivo())
        );
    }

    @Test
    void deberiaAsignarFechasAlCrearYActualizarUnProveedor() {
        Proveedor proveedor = new Proveedor();

        proveedor.onCreate();
        LocalDateTime creada = proveedor.getFechaCreacion();
        proveedor.setFechaActualizacion(LocalDateTime.of(2020, 1, 1, 0, 0));
        proveedor.onUpdate();

        assertAll(
                () -> assertNotNull(creada),
                () -> assertTrue(proveedor.getFechaActualizacion().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)))
        );
    }

    @Test
    void deberiaExponerLosAtributosDeLosDatosDeCatalogo() {
        CategoriaServicio categoria = new CategoriaServicio();
        categoria.setIdCategoria(1L);
        categoria.setNombre("SALUD");
        categoria.setEstado("ACTIVO");
        TipoServicio tipo = new TipoServicio();
        tipo.setCategoria(categoria);
        tipo.setNombre("Odontologia");
        DisponibilidadServicio disponibilidad = new DisponibilidadServicio();
        disponibilidad.setDiaSemana("LUNES");

        assertAll(
                () -> assertEquals("SALUD", tipo.getCategoria().getNombre()),
                () -> assertEquals("Odontologia", tipo.getNombre()),
                () -> assertEquals("LUNES", disponibilidad.getDiaSemana())
        );
    }
}
