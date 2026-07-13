# Documento de Seguridad – Audit Service

## 1. Objetivo del servicio

El **Audit Service** centraliza los eventos relevantes de seguridad y negocio generados por los demás microservicios.

Su propósito es proporcionar trazabilidad, apoyar la detección de actividad sospechosa y conservar evidencia sobre las acciones realizadas dentro de la plataforma.

## 2. Activos protegidos

- Eventos de auditoría.
- Identidad del usuario.
- Acción realizada.
- Recurso afectado.
- Fecha y hora.
- Dirección de origen.
- Correlation ID.
- Resultado de la operación.
- Evidencia de integridad.
- Alertas de actividad sospechosa.

## 3. Componentes de seguridad

### 3.1 Registro de eventos

Cada evento debe contener, cuando corresponda:

- Identificador único.
- Servicio de origen.
- Usuario.
- Acción.
- Tipo de recurso.
- Identificador del recurso.
- Resultado.
- Fecha y hora.
- Correlation ID.
- Dirección IP o contexto.
- Datos adicionales controlados.

### 3.2 Integridad de eventos

Los eventos pueden protegerse mediante una firma o código de autenticación basado en un secreto, como `AUDIT_INTEGRITY_SECRET`.

**Objetivo:** detectar modificaciones no autorizadas en los registros.

El secreto debe:

- Generarse aleatoriamente.
- Mantenerse fuera del repositorio.
- Cargarse desde variables de entorno.
- Tener longitud suficiente.
- Rotarse según política.

### 3.3 Detección de actividad sospechosa

El servicio analiza eventos como:

- Múltiples intentos fallidos.
- Acceso repetido a recursos prohibidos.
- Consultas masivas.
- Cambios administrativos anómalos.
- Reutilización de tokens.
- Fallos reiterados en una ventana temporal.

Configuraciones relacionadas:

```text
AUDIT_FAILURE_THRESHOLD
AUDIT_DETECTION_WINDOW_MINUTES
```

### 3.4 Rate limiting interno

Las operaciones de escritura realizadas por microservicios deben limitarse para evitar saturación accidental o maliciosa.

```text
AUDIT_INTERNAL_RATE_LIMIT
```

### 3.5 Rate limiting de consultas

Los endpoints de consulta deben aplicar límites más estrictos.

```text
AUDIT_QUERY_RATE_LIMIT
```

### 3.6 Autenticación entre servicios

Los endpoints internos no deben confiar únicamente en cabeceras como `user-email`.

Deben incluir un mecanismo de autenticación del servicio solicitante, como:

- Token interno.
- mTLS.
- Clave rotativa.
- Red privada más autenticación de aplicación.

### 3.7 Sanitización de logs

Los valores recibidos deben limpiarse para evitar:

- Log injection.
- Saltos de línea maliciosos.
- Contenido excesivo.
- Inclusión de tokens completos.
- Contraseñas.
- Claves.
- Datos personales innecesarios.

### 3.8 Consulta de auditoría

Las consultas deben restringirse a administradores o usuarios autorizados.

También deben aplicarse:

- Paginación.
- Límites máximos.
- Filtros validados.
- Rango de fechas.
- Ordenamiento permitido.
- Protección contra consultas costosas.

### 3.9 Inmutabilidad lógica

Los eventos no deben exponerse mediante endpoints públicos de actualización o eliminación.

Cuando exista una política de retención, la eliminación debe ejecutarse mediante procesos administrativos controlados.

### 3.10 Manejo de fallos

Si Audit Service no está disponible, los servicios de negocio deben usar una estrategia definida:

- Outbox.
- Cola.
- Reintento.
- Circuit breaker.
- Registro local temporal.

No debe perderse silenciosamente la evidencia.

## 4. Endpoints críticos

| Endpoint | Acceso esperado |
|---|---|
| Registro interno de evento | Solo microservicios autenticados |
| Consulta de eventos | Administrador o auditor |
| Consulta por usuario | Acceso restringido |
| Consulta por correlación | Acceso restringido |
| Verificación de integridad | Administrador |
| Consulta de alertas | Administrador o auditor |

## 5. Configuración segura

- Secreto de integridad obligatorio.
- Base de datos no pública.
- Usuario de BD con mínimo privilegio.
- Límites de consulta.
- Límite de tamaño de eventos.
- Retención definida.
- Zona horaria consistente.
- Correlation ID obligatorio.
- Health endpoint sin datos sensibles.

## 6. Pruebas de seguridad

- Crear evento válido.
- Crear evento sin autenticación interna.
- Alterar un evento y verificar integridad.
- Inyectar salto de línea.
- Enviar token dentro de detalles.
- Superar umbral de fallos.
- Superar rate limit interno.
- Superar rate limit de consulta.
- Consultar sin permiso.
- Solicitar rango excesivo.
- Comprobar paginación.
- Simular indisponibilidad.

## 7. Evidencias

- Servicio de integridad.
- Servicio de detección.
- Configuración de límites.
- Endpoint interno.
- Pruebas de manipulación.
- Capturas de alertas.
- Registros con correlation ID.
