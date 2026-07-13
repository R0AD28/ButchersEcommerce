# Documento de Seguridad – Catalog Service

## 1. Objetivo del servicio

El **Catalog Service** administra la información de productos de Butchers Ecommerce. Permite consultar, crear y actualizar productos aplicando controles de autenticación, autorización, validación de entradas y auditoría.

Este servicio no genera la identidad del usuario. Su responsabilidad es validar el token emitido por el Auth Service y aplicar los permisos obtenidos desde la fuente autorizada.

## 2. Activos protegidos

- Productos.
- Precios.
- Descripciones.
- Estado de productos.
- Historial de modificaciones.
- Identidad del usuario que realiza cambios.
- Permisos relacionados con el catálogo.

## 3. Componentes de seguridad

### 3.1 Validación del token

El servicio recibe un token generado por el Auth Service y verifica:

- Firma.
- Expiración.
- Emisor.
- Audiencia.
- Formato.
- Identidad del usuario.

El Catalog Service no debe emitir tokens nuevos.

### 3.2 Construcción del contexto de autenticación

Después de validar el token, el servicio crea el contexto de autenticación de Spring Security con la identidad confirmada.

Los permisos aplicables deben obtenerse desde la fuente central definida por la arquitectura.

### 3.3 Autorización por permisos

Los principales permisos del servicio son:

| Permiso | Operación |
|---|---|
| `VIEW_PRODUCTS` | Consultar productos |
| `CREATE_PRODUCT` | Crear productos |
| `UPDATE_PRODUCT` | Actualizar productos |

Los endpoints deben utilizar controles explícitos como `@PreAuthorize`.

### 3.4 Validación de DTO

Los datos recibidos deben validarse antes de llegar a la capa de persistencia.

Validaciones mínimas:

- Nombre obligatorio.
- Longitud máxima.
- Descripción controlada.
- Precio mayor o igual a cero.
- Campos desconocidos rechazados cuando corresponda.
- Identificador válido.
- Tipos de datos correctos.

### 3.5 Prevención de inyección

El uso de JPA y repositorios parametrizados reduce el riesgo de SQL Injection. No deben construirse consultas mediante concatenación de datos enviados por el usuario.

### 3.6 Prevención de XSS

Los textos almacenados, como nombre y descripción, deben tratarse como datos y no como código ejecutable.

El frontend debe escapar el contenido antes de renderizarlo. El backend debe validar longitudes y formatos y evitar almacenar contenido innecesariamente peligroso.

### 3.7 Separación entre DTO y entidad

Los controladores deben recibir DTO y no entidades persistentes completas.

**Objetivo:** prevenir mass assignment y evitar que el usuario modifique campos internos como:

- Identificador.
- Fecha de creación.
- Fecha de actualización.
- Versión.
- Usuario creador.
- Estado interno.

### 3.8 Auditoría de operaciones

El servicio debe registrar:

- Consulta sensible, cuando aplique.
- Creación de producto.
- Actualización de producto.
- Intentos no autorizados.
- Errores relevantes.
- Usuario responsable.
- Recurso afectado.
- Fecha y hora.
- Correlation ID.

### 3.9 Manejo de errores

Las respuestas deben diferenciar:

- Producto inexistente: `404`.
- Datos inválidos: `400`.
- Token inválido: `401`.
- Permiso insuficiente: `403`.
- Conflicto de actualización: `409`.
- Exceso de solicitudes: `429`.

### 3.10 Control de concurrencia

Cuando varios usuarios modifican un producto, debe impedirse la pérdida silenciosa de actualizaciones.

Puede utilizarse versionamiento optimista mediante `@Version`.

## 4. Endpoints críticos

| Endpoint | Permiso | Riesgo |
|---|---|---|
| `GET /products` | `VIEW_PRODUCTS` | Exposición no autorizada |
| `GET /products/{id}` | `VIEW_PRODUCTS` | Enumeración o IDOR |
| `POST /products` | `CREATE_PRODUCT` | Creación no autorizada |
| `PUT /products/{id}` | `UPDATE_PRODUCT` | Manipulación de precios |
| `DELETE /products/{id}` | Permiso específico | Eliminación indebida |

## 5. Configuración segura

- Validación de issuer y audience.
- Clave pública configurable.
- Endpoints públicos mínimos.
- CORS restringido.
- Logs sin tokens completos.
- Base de datos no expuesta públicamente.
- Usuario de base de datos con privilegios mínimos.

## 6. Pruebas de seguridad

- Consulta con permiso válido.
- Consulta sin token.
- Consulta con token inválido.
- Creación como cliente.
- Creación como vendedor.
- Actualización sin permiso.
- Precio negativo.
- Nombre vacío.
- Payload con campos internos.
- SQL Injection.
- XSS almacenado.
- Modificación concurrente.
- Acceso a producto inexistente.

## 7. Evidencias

- `SecurityConfig`.
- Filtro JWT.
- Controladores con `@PreAuthorize`.
- DTO con validaciones.
- `GlobalExceptionHandler`.
- Pruebas unitarias.
- Pruebas de integración.
- Reporte estático.
- Reporte ZAP.
