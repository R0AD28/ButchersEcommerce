# audit-service

Microservicio centralizado e inmutable de auditoría para ButchersEcommerce.

## Endpoints

- `POST /internal/audit-events`: escritura interna protegida con `X-Service-Name` y `X-Service-Token`.
- `GET /audit-events`: consulta paginada protegida con JWT y permiso `VIEW_AUDIT_LOGS`.
- `GET /actuator/health`: health check.

## Decisiones de seguridad

1. El servicio solo posee la clave pública RSA.
2. El JWT contiene identidad; los privilegios actuales se consultan en auth-service.
3. Los eventos internos no requieren JWT porque login fallido y registro pueden ocurrir sin usuario autenticado.
4. El nombre del servicio se obtiene del header interno validado, no del cuerpo recibido.
5. No existen endpoints de actualización o eliminación de auditorías.
6. Las consultas están paginadas y limitadas a 100 elementos.
7. Los errores no devuelven stack traces.

## Permiso requerido en auth-service

Agregar `VIEW_AUDIT_LOGS` y asignarlo al rol `ADMINISTRADOR`.

## Compilar

```bash
./mvnw clean verify
```

## Construir imagen

```bash
docker build -t audit-service .
```

## Controles de seguridad adicionales

- Cadena HMAC-SHA256 (`previous_hash` y `event_hash`).
- Endpoint `GET /audit-events/integrity` con `VERIFY_AUDIT_INTEGRITY`.
- Detección de eventos repetidos y clasificación `critical`.
- Alertas de seguridad sin imprimir secretos.
- Sanitización del campo `detail`.
- Auditoría de las consultas realizadas por auditores.
- Rate limiting local para escritura interna y consultas.
- Script de privilegios append-only en `db/security/audit_app_permissions.sql`.

Variables nuevas:

```text
AUDIT_INTEGRITY_SECRET=<mínimo 32 caracteres aleatorios>
AUDIT_FAILURE_THRESHOLD=5
AUDIT_DETECTION_WINDOW_MINUTES=5
AUDIT_INTERNAL_RATE_LIMIT=300
AUDIT_QUERY_RATE_LIMIT=30
```

Permisos que deben existir en auth-service:

```text
VIEW_AUDIT_LOGS
VERIFY_AUDIT_INTEGRITY
```
