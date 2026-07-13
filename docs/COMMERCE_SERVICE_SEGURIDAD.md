# Documento de Seguridad – Commerce Service

## 1. Objetivo del servicio

El **Commerce Service** administra las operaciones comerciales de la aplicación, incluyendo órdenes, pagos, facturas y cambios de estado.

Debido a que procesa información económica y transacciones críticas, incorpora controles de autorización, idempotencia, integridad, concurrencia y auditoría.

## 2. Activos protegidos

- Órdenes.
- Pagos.
- Facturas.
- Estados de órdenes.
- Valores monetarios.
- Identidad del cliente.
- Correo del cliente.
- Historial de transacciones.
- Claves de idempotencia.

## 3. Componentes de seguridad

### 3.1 Autenticación

El servicio valida el token emitido por Auth Service antes de permitir operaciones protegidas.

### 3.2 Autorización

Las operaciones deben restringirse según el permiso requerido, por ejemplo:

- Crear orden.
- Ver órdenes propias.
- Ver todas las órdenes.
- Actualizar estado.
- Registrar pago.
- Consultar factura.

### 3.3 Autorización por propiedad

Un usuario no debe acceder a órdenes de otro cliente únicamente cambiando el identificador de la URL.

La validación debe comprobar:

- Identidad autenticada.
- Propietario de la orden.
- Rol administrativo o permiso especial.
- Relación entre recurso y usuario.

### 3.4 Idempotencia

Las operaciones críticas deben aceptar una clave de idempotencia.

**Objetivo:** impedir que una solicitud repetida produzca múltiples pagos, órdenes o facturas.

El servicio debe:

- Registrar la clave.
- Asociarla con la operación.
- Rechazar reutilizaciones incompatibles.
- Devolver el mismo resultado cuando corresponda.
- Aplicar expiración o limpieza según política.

### 3.5 Integridad de valores

Los valores finales no deben depender exclusivamente de datos enviados por el cliente.

El backend debe recalcular:

- Precio.
- Subtotal.
- Impuestos.
- Total.
- Cantidad.
- Descuentos autorizados.

### 3.6 Control de estados

Las transiciones deben encontrarse explícitamente definidas.

Ejemplo:

```text
CREATED -> PAID -> PROCESSING -> SHIPPED -> COMPLETED
```

No deben permitirse transiciones arbitrarias, como:

```text
COMPLETED -> CREATED
```

### 3.7 Control de concurrencia

Se debe utilizar versionamiento optimista o bloqueo transaccional para impedir modificaciones simultáneas inconsistentes.

### 3.8 Validación de pagos

El servicio debe evitar:

- Pagos con valores negativos.
- Pago de una orden inexistente.
- Pago duplicado.
- Pago de una orden ya pagada.
- Modificación del total desde el cliente.
- Facturación sin pago confirmado.

### 3.9 Auditoría

Se deben registrar:

- Creación de orden.
- Cambio de estado.
- Intento de cambio inválido.
- Registro de pago.
- Pago duplicado.
- Emisión de factura.
- Rechazo por idempotencia.
- Usuario responsable.
- Correlation ID.

### 3.10 Outbox de auditoría

Cuando se utiliza un patrón Outbox, el evento de auditoría se almacena dentro de la misma transacción de negocio y se publica posteriormente.

**Objetivo:** evitar que una operación se confirme sin conservar la evidencia de auditoría.

## 4. Endpoints críticos

| Operación | Riesgo | Control |
|---|---|---|
| Crear orden | Manipulación de precio | Recálculo en backend |
| Consultar orden | IDOR | Validación de propiedad |
| Actualizar estado | Escalamiento de privilegios | Permiso específico |
| Registrar pago | Duplicación | Idempotencia |
| Generar factura | Fraude o inconsistencia | Estado válido |
| Listar órdenes | Exposición de datos | Filtros por usuario |

## 5. Manejo de errores

- `400`: datos inválidos.
- `401`: token inválido.
- `403`: acceso a orden ajena.
- `404`: orden o factura inexistente.
- `409`: pago duplicado, versión desactualizada o transición inválida.
- `422`: operación semánticamente inválida, cuando se utilice.
- `429`: exceso de solicitudes.

## 6. Pruebas de seguridad

- Crear orden con token válido.
- Crear orden sin token.
- Manipular total.
- Consultar orden de otro usuario.
- Repetir pago con la misma clave.
- Repetir pago con clave diferente.
- Pago negativo.
- Cambio de estado no permitido.
- Cambio de estado sin permiso.
- Modificación concurrente.
- Caída temporal del Audit Service.
- Confirmación del evento Outbox.

## 7. Evidencias

- `OrderService`.
- `PaymentService`.
- `InvoiceService`.
- `IdempotencyService`.
- `AuditOutboxService`.
- Controladores protegidos.
- Pruebas unitarias.
- Pruebas de integración.
- Evidencias de transacciones.
