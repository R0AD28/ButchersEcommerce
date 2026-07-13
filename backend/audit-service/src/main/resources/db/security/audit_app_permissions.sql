-- Ejecutar como administrador de PostgreSQL DESPUÉS de Flyway.
-- Sustituye audit_app_user por el usuario real de ejecución.
REVOKE UPDATE, DELETE, TRUNCATE ON audit_events FROM audit_app_user;
GRANT SELECT, INSERT ON audit_events TO audit_app_user;

-- Flyway debe utilizar un usuario separado con permisos de migración.
