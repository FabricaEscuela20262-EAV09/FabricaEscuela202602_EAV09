-- Hibernate ejecuta este archivo después de crear el esquema en las pruebas de integración (ddl-auto=create-drop).
-- Replica la restricción ex_reserva_horario de ESTRUCTURAS.sql documentada en el README (sección 6.2, ADR-09):
-- dos reservas PENDIENTE o CONFIRMADA de la misma oferta no pueden solaparse en el intervalo [inicio, fin).
-- Cada sentencia debe ocupar una sola línea.
ALTER TABLE "RESERVA" ADD CONSTRAINT ex_reserva_horario EXCLUDE USING gist (id_servicio WITH =, tsrange(fecha_inicio, fecha_fin, '[)') WITH &&) WHERE (estado IN ('PENDIENTE', 'CONFIRMADA'));
