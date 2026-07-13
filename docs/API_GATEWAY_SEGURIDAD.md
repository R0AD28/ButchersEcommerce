# Documento de Seguridad – API Gateway

## 1. Objetivo del componente

El **API Gateway** es el punto de entrada principal a Butchers Ecommerce. Centraliza controles transversales antes de enrutar las solicitudes hacia los microservicios.

## 2. Activos protegidos

- Endpoints públicos.
- Microservicios internos.
- Tokens.
- Cabeceras.
- Rutas.
- Disponibilidad de la API.
- Identificadores de correlación.
- Configuración CORS.

## 3. Componentes de seguridad

### 3.1 Terminación HTTPS

El Gateway debe exponer la aplicación mediante HTTPS para proteger la confidencialidad e integridad de las comunicaciones.

### 3.2 Validación inicial de tokens

Las rutas protegidas deben verificar la existencia y formato del token antes de enrutar la solicitud.

La validación completa también puede repetirse en cada microservicio como defensa en profundidad.

### 3.3 Enrutamiento seguro

Solo deben publicarse las rutas necesarias. Los endpoints internos no deben exponerse directamente.

### 3.4 CORS

La política debe permitir únicamente los orígenes requeridos por el frontend desplegado.

No se recomienda utilizar `*` junto con credenciales.

### 3.5 Rate limiting

Debe aplicarse según:

- Dirección IP.
- Usuario.
- Ruta.
- Tipo de operación.
- Ventana temporal.

Los endpoints de login, refresh, auditoría y operaciones críticas deben tener políticas específicas.

### 3.6 Cabeceras de seguridad

El Gateway debe aplicar, según corresponda:

- `Strict-Transport-Security`.
- `X-Content-Type-Options`.
- `Content-Security-Policy`.
- `Referrer-Policy`.
- `Permissions-Policy`.
- Protección contra framing.

### 3.7 Correlation ID

Cada solicitud debe tener un identificador único, generado o propagado por el Gateway.

Este valor debe enviarse a todos los servicios y registrarse en auditoría.

### 3.8 Límite de tamaño

Se deben limitar:

- Cuerpo de solicitud.
- Cabeceras.
- Archivos.
- Longitud de URL.
- Cantidad de parámetros.

### 3.9 Restricción de métodos

Cada ruta debe aceptar únicamente los métodos necesarios.

### 3.10 Ocultamiento de infraestructura

El Gateway no debe revelar:

- Versiones internas.
- Nombres de contenedores.
- Direcciones privadas.
- Stack traces.
- Tecnologías innecesarias.

## 4. Rutas

| Ruta | Destino | Protección |
|---|---|---|
| `/auth/**` | Auth Service | Pública o limitada según endpoint |
| `/products/**` | Catalog Service | Token y permisos |
| `/orders/**` | Commerce Service | Token y autorización |
| `/payments/**` | Commerce Service | Token, permiso e idempotencia |
| `/audit/**` | Audit Service | Acceso administrativo |
| `/internal/**` | Servicios internos | No pública |

## 5. Pruebas de seguridad

- Acceso HTTP y redirección a HTTPS.
- CORS desde origen permitido.
- CORS desde origen no permitido.
- Token ausente.
- Token manipulado.
- Ruta inexistente.
- Método no permitido.
- Payload excesivo.
- Rate limit.
- Cabeceras de seguridad.
- Acceso directo a ruta interna.
- Propagación del correlation ID.

## 6. Evidencias

- Configuración de rutas.
- Filtros globales.
- Configuración CORS.
- Configuración de rate limiting.
- Captura de HTTPS.
- Resultado de pruebas ZAP.
- Logs distribuidos con correlation ID.
