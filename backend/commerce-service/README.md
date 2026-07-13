# commerce-service

Alcance reducido y prioritario del microservicio de comercio:

- Creación de órdenes con precios consultados desde catalog-service.
- Validación de propietario del recurso.
- Idempotencia para creación de órdenes y pagos.
- Pago simulado mediante token externo; nunca guarda datos de tarjeta.
- Factura básica generada únicamente después de un pago aprobado.
- Promociones con vigencia, estado y porcentaje controlado en backend.
- JWT RS256 y permisos actuales consultados a auth-service.
- Auditoría transaccional mediante outbox.
- Sin módulo de entregas, según el alcance reducido.

## Permisos

- CLIENTE: CREATE_ORDER, VIEW_OWN_ORDERS, PROCESS_PAYMENT, VIEW_OWN_INVOICES.
- ADMINISTRADOR: MANAGE_PROMOTIONS.

## Endpoints

- POST /orders
- GET /orders/mine
- GET /orders/{orderNumber}
- POST /payments
- GET /invoices/{orderNumber}
- POST /promotions
- GET /promotions

POST /orders y POST /payments requieren Idempotency-Key.

## Pruebas incluidas

- creación con precios provenientes del catálogo;
- rechazo de SKU duplicado;
- producto no disponible;
- control de propietario;
- pago aprobado y factura;
- pago rechazado sin factura;
- rechazo de segundo pago;
- promoción válida, vencida y fechas inválidas.

Ejecutar:

```powershell
.\mvnw.cmd clean test
```
