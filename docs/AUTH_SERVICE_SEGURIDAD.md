# Documento de Seguridad – Auth Service

## 1. Objetivo del servicio

El **Auth Service** centraliza la autenticación de usuarios, la emisión y renovación de tokens, la administración de roles y permisos, y el control de sesiones de la plataforma Butchers Ecommerce.

Su propósito es garantizar que únicamente usuarios identificados y autenticados puedan acceder a los recursos protegidos y que las operaciones administrativas se encuentren restringidas según los permisos almacenados en la base de datos.

## 2. Activos protegidos

- Credenciales de usuarios.
- Contraseñas cifradas.
- Identidad de los usuarios.
- Roles y permisos.
- Access tokens.
- Refresh tokens.
- Claves criptográficas.
- Sesiones activas.
- Eventos de autenticación y autorización.

## 3. Componentes de seguridad

### 3.1 Validación de credenciales

El servicio compara las credenciales proporcionadas por el usuario con las almacenadas en la base de datos. Las contraseñas no deben almacenarse en texto plano, sino mediante un algoritmo de hash seguro.

**Objetivo de seguridad:** evitar la exposición directa de contraseñas y reducir el impacto de una filtración de base de datos.

**Amenazas mitigadas:**

- Suplantación de identidad.
- Robo de credenciales.
- Acceso no autorizado.

### 3.2 Generación de access tokens

Después de una autenticación exitosa, el servicio genera un token firmado que contiene la identidad mínima necesaria del usuario.

En la arquitectura del proyecto, el token se utiliza principalmente como evidencia de identidad. Los roles y permisos no deben considerarse confiables únicamente por estar incluidos en el token, ya que la autorización se obtiene desde la fuente central correspondiente.

**Validaciones esperadas:**

- Firma criptográfica.
- Fecha de emisión.
- Fecha de expiración.
- Emisor.
- Audiencia.
- Identidad del usuario.

### 3.3 Firma criptográfica de tokens

Los tokens se firman con una clave privada y son verificados mediante la clave pública correspondiente.

**Objetivo de seguridad:** impedir que un atacante pueda crear o modificar tokens válidos.

**Amenazas mitigadas:**

- Manipulación de tokens.
- Suplantación de usuarios.
- Elevación de privilegios.

### 3.4 Carga segura de claves PEM

Las claves criptográficas se cargan desde rutas configurables o recursos seguros. Las claves privadas no deben almacenarse directamente en el repositorio público.

**Configuraciones relacionadas:**

```text
SECURITY_JWT_PRIVATE_KEY_PATH
SECURITY_JWT_PUBLIC_KEY_PATH
SECURITY_JWT_ISSUER
SECURITY_JWT_AUDIENCE
SECURITY_JWT_EXPIRATION
```

### 3.5 Refresh tokens

Los refresh tokens permiten renovar el access token sin solicitar nuevamente las credenciales.

El servicio debe verificar:

- Existencia del token.
- Fecha de expiración.
- Estado de revocación.
- Asociación con el usuario.
- Reutilización indebida.

### 3.6 Revocación de sesión

Durante el cierre de sesión, el refresh token debe invalidarse para impedir que sea utilizado nuevamente.

**Resultado esperado:** una sesión cerrada no puede renovarse.

### 3.7 Administración de roles

La asignación de roles se limita a usuarios con permisos administrativos, por ejemplo `MANAGE_ROLES`.

El servicio debe controlar:

- Usuario objetivo existente.
- Rol solicitado existente.
- Permiso del administrador.
- Reemplazo o acumulación permitida de roles.
- Registro de la modificación.

### 3.8 Recuperación de roles y permisos

Los roles y permisos se consultan desde la base de datos. Esto permite modificar privilegios sin depender exclusivamente de la información contenida en tokens previamente emitidos.

### 3.9 Protección de endpoints

Los endpoints se clasifican como:

- Públicos.
- Requieren autenticación.
- Requieren un rol.
- Requieren un permiso específico.
- Uso interno entre servicios.

### 3.10 Manejo seguro de errores

El servicio debe evitar devolver:

- Stack traces.
- Contraseñas.
- Claves.
- Detalles internos de consultas.
- Diferencias que permitan enumerar usuarios.

Las respuestas deben utilizar códigos HTTP apropiados:

| Código | Uso |
|---|---|
| 400 | Solicitud inválida |
| 401 | Usuario no autenticado o token inválido |
| 403 | Usuario autenticado sin permiso |
| 404 | Recurso inexistente |
| 409 | Conflicto |
| 429 | Exceso de solicitudes |
| 500 | Error interno controlado |

### 3.11 Auditoría de autenticación

El servicio debe registrar:

- Inicio de sesión exitoso.
- Inicio de sesión fallido.
- Generación de token.
- Renovación de token.
- Cierre de sesión.
- Asignación de roles.
- Intentos administrativos no autorizados.

## 4. Endpoints críticos

| Endpoint | Riesgo principal | Control esperado |
|---|---|---|
| Registro | Datos inválidos o cuentas duplicadas | Validación y restricciones |
| Login | Fuerza bruta | Rate limiting y auditoría |
| Token | Reutilización de códigos | Expiración y uso único |
| Refresh | Token robado | Revocación y expiración |
| Logout | Sesión reutilizable | Invalidación |
| Assign Role | Elevación de privilegios | `MANAGE_ROLES` |

## 5. Configuración segura

- Secretos mediante variables de entorno.
- Claves privadas fuera del repositorio.
- Perfiles separados para desarrollo, pruebas y producción.
- Expiración limitada de tokens.
- CORS restringido.
- Actuator protegido.
- Swagger desactivado o protegido en producción.
- Logs sin datos sensibles.

## 6. Pruebas de seguridad asociadas

- Login válido.
- Login inválido.
- Fuerza bruta.
- Token expirado.
- Token manipulado.
- Token con issuer incorrecto.
- Token con audience incorrecta.
- Refresh token revocado.
- Usuario sin permiso administrativo.
- Asignación de rol por administrador.
- Acceso con usuario deshabilitado.

## 7. Evidencias

- Código de configuración de Spring Security.
- Filtro de autenticación.
- Servicio de tokens.
- Migraciones de roles y permisos.
- Pruebas unitarias.
- Pruebas de integración.
- Capturas de Postman.
- Resultados de SonarCloud y SpotBugs.
- Registros del Audit Service.
