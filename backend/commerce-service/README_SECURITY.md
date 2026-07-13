# Seguridad implementada en catalog-service

- JWT RS256 validado con clave pública, issuer, audience, expiración, jti e identidad.
- Permisos actuales consultados a auth-service; el JWT no transporta roles ni permisos.
- Endpoints públicos de catálogo no exponen stock exacto, estado interno ni timestamps.
- Vista de gestión separada y protegida por permisos de escritura.
- Paginación con máximo de 100 registros.
- Control de concurrencia optimista (`@Version`) y `expectedVersion` en cambios.
- Inventario separado de la edición comercial.
- `Idempotency-Key` obligatorio para cambios de inventario.
- Rate limiting Redis por IP, método y endpoint.
- Auditoría transaccional mediante Outbox con reintentos.
- Auditoría de 401 y 403.
- Timeouts para auth-service y audit-service.
- CORS restrictivo, CSP, HSTS, anti-clickjacking, nosniff y Referrer-Policy.
- Límite de tamaño de solicitudes.

## Endpoints sin IDs en URL

- `GET /products?page=0&size=20`
- `GET /products/detail?sku=CARNE-001`
- `GET /products/manage?includeInactive=true&page=0&size=20`
- `POST /products`
- `PUT /products`
- `PATCH /products/inventory` con `Idempotency-Key`
- `PATCH /products/status`

## Variables nuevas para Docker Compose

```yaml
REDIS_HOST: redis
REDIS_PORT: 6379
CATALOG_RATE_LIMIT_READ: 100
CATALOG_RATE_LIMIT_WRITE: 30
CATALOG_RATE_LIMIT_WINDOW: 60
IDEMPOTENCY_TTL_HOURS: 24
HTTP_CONNECT_TIMEOUT_MS: 2000
HTTP_READ_TIMEOUT_MS: 3000
AUDIT_OUTBOX_DELAY_MS: 5000
```
