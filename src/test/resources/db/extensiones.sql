-- Extensiones que exige el esquema real (ESTRUCTURAS.sql) y que deben existir antes de que
-- Hibernate cree las tablas: citext (correo sin distinguir mayúsculas) y btree_gist (EXCLUDE USING gist).
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS btree_gist;
