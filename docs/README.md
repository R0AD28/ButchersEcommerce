# Documentación de Seguridad por Microservicio

Esta carpeta contiene la documentación de los mecanismos de seguridad implementados en cada componente principal de Butchers Ecommerce.

## Documentos incluidos

1. `AUTH_SERVICE_SEGURIDAD.md`
   - Autenticación.
   - Tokens.
   - Claves.
   - Roles.
   - Permisos.
   - Sesiones.

2. `CATALOG_SERVICE_SEGURIDAD.md`
   - Validación de identidad.
   - Autorización por permisos.
   - Validación de productos.
   - Auditoría.

3. `COMMERCE_SERVICE_SEGURIDAD.md`
   - Órdenes.
   - Pagos.
   - Facturación.
   - Idempotencia.
   - Concurrencia.
   - Integridad transaccional.

4. `AUDIT_SERVICE_SEGURIDAD.md`
   - Registro de eventos.
   - Integridad.
   - Detección de actividad sospechosa.
   - Rate limiting.
   - Consultas seguras.

5. `API_GATEWAY_SEGURIDAD.md`
   - HTTPS.
   - CORS.
   - Rate limiting.
   - Cabeceras.
   - Enrutamiento seguro.
   - Correlation ID.

## Ubicación sugerida

```text
ButchersEcommerce/
├── README.md
├── docs/
│   └── seguridad/
│       ├── README.md
│       ├── AUTH_SERVICE_SEGURIDAD.md
│       ├── CATALOG_SERVICE_SEGURIDAD.md
│       ├── COMMERCE_SERVICE_SEGURIDAD.md
│       ├── AUDIT_SERVICE_SEGURIDAD.md
│       └── API_GATEWAY_SEGURIDAD.md
└── backend/
```

Cada documento debe actualizarse cuando se modifique un control de seguridad, endpoint, permiso, variable de entorno o mecanismo de auditoría.
