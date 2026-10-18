package com.reservar.dto.servicio;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validaciones de formato de los DTO de la Épica 2 (campo obligatorio vacío, límites de longitud). */
class ServicioRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static CrearOfertaRequest crearValido() {
        CrearOfertaRequest request = new CrearOfertaRequest();
        request.setIdTipoServicio(2L);
        request.setNombre("Limpieza dental");
        request.setDescripcion("Limpieza completa");
        request.setDuracionMinutos(45);
        request.setModalidad("PRESENCIAL");
        request.setUbicacion("Villavicencio");
        return request;
    }

    private static ActualizarOfertaRequest actualizarValido() {
        ActualizarOfertaRequest request = new ActualizarOfertaRequest();
        request.setIdTipoServicio(2L);
        request.setNombre("Limpieza dental");
        request.setDescripcion("Limpieza completa");
        request.setDuracionMinutos(45);
        request.setModalidad("REMOTO");
        return request;
    }

    private static <T> Set<String> campos(Set<ConstraintViolation<T>> violaciones) {
        return violaciones.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static <T> String mensajeDe(Set<ConstraintViolation<T>> violaciones, String campo) {
        return violaciones.stream()
                .filter(v -> v.getPropertyPath().toString().equals(campo))
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse(null);
    }

    @Test
    void deberiaAceptarUnaSolicitudDePublicacionCompleta() {
        assertTrue(validator.validate(crearValido()).isEmpty());
    }

    @Test
    void deberiaAceptarUnaPublicacionRemotaSinUbicacion() {
        CrearOfertaRequest request = crearValido();
        request.setModalidad("REMOTO");
        request.setUbicacion(null);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void deberiaSenalarCadaCampoObligatorioVacioEnLaPublicacion() {
        CrearOfertaRequest request = new CrearOfertaRequest();

        Set<ConstraintViolation<CrearOfertaRequest>> violaciones = validator.validate(request);

        assertEquals(
                Set.of("idTipoServicio", "nombre", "descripcion", "duracionMinutos", "modalidad"),
                campos(violaciones));
        assertEquals("Debe ingresar nombre de la oferta", mensajeDe(violaciones, "nombre"));
        assertEquals("Debe ingresar descripción de la oferta", mensajeDe(violaciones, "descripcion"));
        assertEquals("Debe ingresar duración para la oferta", mensajeDe(violaciones, "duracionMinutos"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void deberiaRechazarTextosEnBlancoEnLaPublicacion(String blanco) {
        CrearOfertaRequest request = crearValido();
        request.setNombre(blanco);
        request.setDescripcion(blanco);
        request.setModalidad(blanco);

        assertEquals(Set.of("nombre", "descripcion", "modalidad"), campos(validator.validate(request)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void deberiaRechazarTipoDeServicioNoPositivo(long id) {
        CrearOfertaRequest request = crearValido();
        request.setIdTipoServicio(id);

        Set<ConstraintViolation<CrearOfertaRequest>> violaciones = validator.validate(request);

        assertEquals("El tipo de servicio ingresado debe ser válido", mensajeDe(violaciones, "idTipoServicio"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -30})
    void deberiaRechazarDuracionNoPositiva(int duracion) {
        CrearOfertaRequest request = crearValido();
        request.setDuracionMinutos(duracion);

        assertEquals("La duración de la oferta debe ser mayor que cero",
                mensajeDe(validator.validate(request), "duracionMinutos"));
    }

    @Test
    void deberiaRechazarNombreMayorA150CaracteresYAceptarExactamente150() {
        CrearOfertaRequest request = crearValido();
        request.setNombre("a".repeat(150));
        assertTrue(validator.validate(request).isEmpty());

        request.setNombre("a".repeat(151));
        assertEquals("El nombre de la oferta no puede superar 150 caracteres",
                mensajeDe(validator.validate(request), "nombre"));
    }

    @Test
    void deberiaRechazarUbicacionMayorA255Caracteres() {
        CrearOfertaRequest request = crearValido();
        request.setUbicacion("u".repeat(256));

        assertEquals("La ubicación no puede superar 255 caracteres",
                mensajeDe(validator.validate(request), "ubicacion"));
    }

    @Test
    void deberiaAplicarLasMismasReglasEnLaEdicion() {
        assertTrue(validator.validate(actualizarValido()).isEmpty());

        ActualizarOfertaRequest vacia = new ActualizarOfertaRequest();
        assertEquals(
                Set.of("idTipoServicio", "nombre", "descripcion", "duracionMinutos", "modalidad"),
                campos(validator.validate(vacia)));

        ActualizarOfertaRequest larga = actualizarValido();
        larga.setNombre("a".repeat(151));
        larga.setUbicacion("u".repeat(256));
        larga.setDuracionMinutos(-1);
        larga.setIdTipoServicio(0L);
        assertEquals(Set.of("nombre", "ubicacion", "duracionMinutos", "idTipoServicio"),
                campos(validator.validate(larga)));
    }

    @Test
    void deberiaValidarLosCamposObligatoriosDeUnaFranjaSemanal() {
        DisponibilidadRequest request = new DisponibilidadRequest();

        Set<ConstraintViolation<DisponibilidadRequest>> violaciones = validator.validate(request);

        assertEquals(Set.of("diaSemana", "horaInicio", "horaFin"), campos(violaciones));
        assertEquals("Debe ingresar el día de la semana", mensajeDe(violaciones, "diaSemana"));
        assertEquals("Debe ingresar la hora de inicio del servicio", mensajeDe(violaciones, "horaInicio"));
        assertEquals("Debe ingresar la hora de finalización del servicio", mensajeDe(violaciones, "horaFin"));
    }

    @Test
    void deberiaAceptarUnaFranjaSemanalCompleta() {
        DisponibilidadRequest request = new DisponibilidadRequest();
        request.setDiaSemana("LUNES");
        request.setHoraInicio(LocalTime.of(9, 0));
        request.setHoraFin(LocalTime.of(12, 0));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void deberiaExigirSoloLaFechaEnUnBloqueo() {
        BloqueoDisponibilidadRequest sinFecha = new BloqueoDisponibilidadRequest();
        assertEquals(Set.of("fecha"), campos(validator.validate(sinFecha)));

        BloqueoDisponibilidadRequest conFecha = new BloqueoDisponibilidadRequest();
        conFecha.setFecha(LocalDate.of(2030, 12, 25));
        assertTrue(validator.validate(conFecha).isEmpty());
    }
}
