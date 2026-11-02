# Plan de implementación: CU-01 con LDAP y JWT rotables

> Estado: **propuesta para revisar**. No hay código implementado de este plan.
> Rama de trabajo: `feature/cu01-autenticar-usuario`.

## 1. Objetivo

Reemplazar la autenticación provisoria del CU-01 por una real:

1. **Consultores**: usuario y contraseña validados contra un directorio **LDAP**.
2. **Candidatos**: tipo y número de documento más la clave del cuestionario, validados
   contra PostgreSQL (como hoy).
3. **Sesión**: tokens **JWT de acceso de vida corta** más **tokens de refresco rotables**,
   en cookies `HttpOnly`. La API protege sus endpoints según el rol.

## 2. Situación actual

| Parte | Hoy | Problema |
| --- | --- | --- |
| `GestorAutenticacion.autenticarConsultor` | Solo verifica que el usuario exista y esté activo. | No valida contraseña (quedó un `TODO`). |
| `consultor` en la base | Sin columna `password`. | Falta la fuente de credenciales (LDAP). |
| Respuesta de login | `SesionDTO { sujetoId, rol }`. | No hay token: la API no puede saber quién llama. |
| Endpoints de la API | Todos públicos. | Cualquiera puede llamar a `/api/candidatos`. |
| Frontend (rama `-frontend`) | Cookie `gestor-rrhh-sesion` escrita por el navegador; `proxy.ts` solo mira si existe. | Se puede falsificar desde la consola del navegador. |
| Acceso de invitado | Equivale a estar autenticado. | Hay que decidir si sigue (ver §9). |

## 3. Arquitectura propuesta

```
Navegador ──(cookies HttpOnly)──► API Spring Boot :8080 ──► LDAP (consultores)
    │                                     │
    └── Next.js :3000                     └──► PostgreSQL (consultor, candidato,
        proxy.ts verifica el JWT                cuestionario, token_refresco)
        de acceso antes de renderizar
```

- **La API emite y valida los tokens.** El frontend nunca los lee desde JavaScript.
- **Las cookies se comparten entre puertos**: el navegador no separa cookies por puerto,
  así que una cookie de `localhost` emitida por la API (`:8080`) también le llega a
  Next (`:3000`). En producción, frontend y API deben compartir dominio
  (por ejemplo `app.dominio` y `api.dominio` con `Domain=dominio`).
- **`proxy.ts` pasa a verificar la firma del JWT** con la clave pública de la API, en vez
  de solo mirar si existe la cookie. La autorización real igual la hace la API.

## 4. Parte A: directorio LDAP

### 4.1 Servidor

| Opción | Uso | A favor | En contra |
| --- | --- | --- | --- |
| **UnboundID embebido** (recomendada para desarrollo y tests) | Se levanta dentro de la API con `spring.ldap.embedded.*` y un LDIF. | Nadie instala nada; los tests de integración lo usan igual. | No es un LDAP "de verdad"; se reinicia con la API. |
| Contenedor Docker de LDAP | Servicio aparte, como hoy PostgreSQL. | Más parecido a producción. | Otra pieza para levantar; hay que elegir una imagen mantenida al implementarlo. |

**Propuesta**: UnboundID embebido por defecto, y que **cambiar a un LDAP real sea solo
configuración** (`spring.ldap.urls`, `spring.ldap.base`, `spring.ldap.username`,
`spring.ldap.password` por variables de entorno).

### 4.2 Datos

- LDIF de prueba en `api/src/main/resources/ldap/consultores.ldif`, con base
  `dc=deso,dc=local` y `ou=consultores`.
- Cada consultor es un `inetOrgPerson` con `uid` = `consultor.username` de
  `db/02-data.sql` (`ADMIN`, `CONSULTOR2`).
- Contraseñas guardadas **con hash** (`{SSHA}` o `{PBKDF2-...}` según soporte el servidor),
  no en texto plano. El README documenta las contraseñas de prueba.
- Valores con acentos codificados en base64 (`cn:: ...`), como exige LDIF.

### 4.3 Código

| Archivo | Responsabilidad |
| --- | --- |
| `dao/DirectorioConsultores.java` | Interfaz: `boolean credencialesValidas(String username, String contrasena)`. |
| `dao/impl/DirectorioConsultoresLdap.java` | Implementación con `LdapTemplate.authenticate` (búsqueda por `uid` + bind). Usa `EqualsFilter` para escapar el usuario y rechaza contraseñas vacías (un bind vacío es anónimo y LDAP lo aceptaría). |
| `service/GestorAutenticacion.java` | Reemplaza el `TODO` por la llamada al directorio. Mensaje de error genérico, igual para usuario inexistente, inactivo o contraseña incorrecta (no revelar cuál falló). |

Dependencias nuevas: `spring-boot-starter-ldap`, `com.unboundid:unboundid-ldapsdk`
(versiones gestionadas por Spring Boot 4.1.1).

## 5. Parte B: sesión con JWT rotables

### 5.1 Dos tokens

| | Token de acceso | Token de refresco |
| --- | --- | --- |
| Formato | JWT firmado | Valor aleatorio opaco (256 bits) |
| Duración | 15 minutos | 8 horas desde el login (jornada), con renovación |
| Dónde vive | Cookie `acceso`, `HttpOnly`, `SameSite=Lax`, `Path=/` | Cookie `refresco`, `HttpOnly`, `SameSite=Strict`, `Path=/api/auth` |
| En la base | No se guarda | Se guarda **solo su hash SHA-256** |
| Revocable | No (expira solo; por eso dura poco) | Sí |

En producción, las dos cookies llevan también `Secure`.

**Claims del JWT de acceso**: `sub` (id del consultor o candidato), `rol`
(`CONSULTOR` o `CANDIDATO`), `nombre`, `exp`, `iat`, `jti`; para candidatos, también
`cuestionarioId`. Firmado con **RS256**: la API firma con la clave privada y cualquiera
puede verificar con la pública (Next lo necesita en `proxy.ts`).

### 5.2 Rotación del token de refresco

Cada vez que se usa un token de refresco, se invalida y se entrega uno nuevo
("rotación"). Si alguien vuelve a presentar uno ya usado, es señal de robo: se revoca
toda la familia de tokens de esa sesión.

```
login ──► refresco R1 (familia F)
uso R1 ──► R1 marcado usado, se emite R2 (familia F)
uso R2 ──► R2 marcado usado, se emite R3
uso R1 otra vez ──► reutilización detectada: se revoca la familia F completa
                    (R3 deja de servir; el usuario debe volver a iniciar sesión)
```

Tabla nueva en `db/01-schema.sql`:

```sql
CREATE TABLE token_refresco (
    id BIGSERIAL PRIMARY KEY,
    hash_token CHAR(64) NOT NULL,          -- SHA-256 en hexadecimal
    familia UUID NOT NULL,                 -- todos los tokens de una misma sesión
    sujeto_id BIGINT NOT NULL,
    rol VARCHAR(20) NOT NULL,              -- CONSULTOR o CANDIDATO
    emitido_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP,                    -- se completa al rotarlo
    revocado_en TIMESTAMP,                 -- logout o reutilización detectada
    CONSTRAINT uq_token_refresco_hash UNIQUE (hash_token)
);
CREATE INDEX ix_token_refresco_familia ON token_refresco (familia);
```

`sujeto_id` no lleva clave foránea porque puede apuntar a `consultor` o a `candidato`.

### 5.3 Claves de firma y su rotación

- Par RSA cargado desde archivos indicados por variables de entorno
  (`JWT_CLAVE_PRIVADA`, `JWT_CLAVE_PUBLICA`). En desarrollo, si no están definidas,
  la API genera un par al iniciar y lo avisa en el log. Consecuencia: al reiniciar
  la API, todos los usuarios tienen que volver a iniciar sesión.
- Cada JWT lleva `kid` (id de la clave). Para rotar claves, la API sigue aceptando la
  clave anterior durante 15 minutos (la vida de un token de acceso).
- Endpoint `GET /api/auth/jwks.json` con las claves públicas, para que `proxy.ts`
  verifique sin compartir secretos.

### 5.4 Endpoints

| Método | Ruta | Acceso | Hace |
| --- | --- | --- | --- |
| `POST` | `/api/auth/consultor` | Público | Valida contra LDAP; emite cookies `acceso` y `refresco`; devuelve `SesionDTO`. |
| `POST` | `/api/auth/candidato` | Público | Valida contra PostgreSQL; ídem. |
| `POST` | `/api/auth/refresco` | Cookie `refresco` | Rota el token de refresco y emite un nuevo token de acceso. |
| `POST` | `/api/auth/logout` | Cookie `refresco` | Revoca la familia y borra las cookies. |
| `GET` | `/api/auth/sesion` | Autenticado | Devuelve quién es el usuario (para mostrar el avatar). |
| `GET` | `/api/auth/jwks.json` | Público | Claves públicas de firma. |

### 5.5 Spring Security

Dependencias nuevas: `spring-boot-starter-security` y
`spring-boot-starter-oauth2-resource-server` (Spring Security 7.1.1, incluye Nimbus
para firmar y verificar JWT).

- `config/SeguridadConfig.java`: API sin estado (`STATELESS`), sin login por formulario.
  - Públicos: `/api/auth/consultor`, `/api/auth/candidato`, `/api/auth/refresco`,
    `/api/auth/logout`, `/api/auth/jwks.json`.
  - `CONSULTOR`: catálogos, candidatos, evaluaciones y el resto de la gestión.
  - `CANDIDATO`: solo los endpoints para responder su propio cuestionario.
- `BearerTokenResolver` propio que toma el JWT de la cookie `acceso` (en lugar del
  encabezado `Authorization`).
- El CORS se mueve de `WebConfig` a la configuración de seguridad, que es la que lo
  aplica primero. Ya permite credenciales (`allowCredentials(true)`).
- **CSRF**: con cookies, un sitio ajeno podría disparar pedidos con la sesión del
  usuario. Se mitiga con `SameSite` y, además, la API exige el encabezado
  `X-Requested-With` en pedidos que modifican datos (un formulario de otro sitio no
  puede agregarlo sin pasar por CORS).
- Errores 401 y 403 con el mismo formato JSON que hoy (`ErrorResponseDTO`).

### 5.6 Código nuevo en la API

| Archivo | Responsabilidad |
| --- | --- |
| `service/GestorTokens.java` | Emitir JWT de acceso; crear, rotar y revocar tokens de refresco; detectar reutilización. |
| `dao/TokenRefrescoDAO.java` + `dao/impl/TokenRefrescoPostgreSQLDAO.java` | Persistencia de `token_refresco`. |
| `model/entity/TokenRefresco.java` | Entidad de la tabla nueva. |
| `config/ClavesJwtConfig.java` | Carga o genera el par RSA; expone `JwtEncoder` y `JwtDecoder`. |
| `controller/AuthController.java` | Endpoints de §5.4; escribe y borra cookies. |

## 6. Cambios en el frontend

Se hacen en `feature/cu01-autenticar-usuario-frontend` (o al unir ambas ramas).

- `service/api.ts`: `credentials: "include"` en todos los pedidos y encabezado
  `X-Requested-With`. Ante un 401, intentar **una vez** `POST /api/auth/refresco` y
  repetir el pedido; si vuelve a fallar, ir a `/login`.
- `session-provider.tsx`: la sesión se obtiene de `GET /api/auth/sesion`; el navegador
  deja de escribir la cookie de sesión.
- `proxy.ts`: verifica la firma y la expiración del JWT de la cookie `acceso` con la
  clave pública (`/api/auth/jwks.json`). Si el acceso venció pero hay `refresco`, deja
  pasar y el cliente renueva. Dependencia nueva: `jose` (verificación de JWT en el
  runtime de Next).
- Logout: `POST /api/auth/logout` y luego `/login`.
- Mensajes de error del formulario: los que devuelva la API (ya está así).

## 7. Pruebas

| Nivel | Qué se prueba |
| --- | --- |
| Unitarias (JUnit + Mockito) | `GestorAutenticacion` (consultor y candidato); `GestorTokens`: emisión, expiración, rotación, reutilización y revocación de familia. |
| Integración LDAP | `DirectorioConsultoresLdap` contra UnboundID embebido con un LDIF de test: contraseña correcta, incorrecta, vacía, usuario con caracteres especiales en el filtro. |
| Integración web (MockMvc) | Cookies emitidas con sus atributos; endpoints protegidos devuelven 401 sin token y 403 con rol equivocado; `/refresco` rota; logout revoca. |
| Frontend | `npm run lint`, `npm run build` y flujos con Playwright: login, renovación automática, logout, acceso sin sesión. |

Meta del sprint: 80 % de cobertura en `GestorAutenticacion` y `GestorTokens`.

## 8. Orden de trabajo (un commit por paso)

1. `feat(auth): validar consultores contra LDAP` (Parte A + tests).
2. `feat(auth): tabla y DAO de tokens de refresco` (esquema, entidad, DAO).
3. `feat(auth): emitir y rotar tokens JWT` (`GestorTokens`, claves, endpoints).
4. `feat(auth): proteger los endpoints con Spring Security` (reglas por rol, CORS, CSRF).
5. `feat(auth): sesión del frontend con cookies de la API` (rama de frontend).
6. `docs: autenticación, variables de entorno y credenciales de prueba` (README).

Los pasos 1 a 4 se pueden revisar y probar solo con la API (curl o tests).

## 9. Decisiones para el equipo

1. **¿UnboundID embebido o LDAP en Docker?** Propuesta: embebido.
2. **Duraciones**: ¿15 min de acceso y 8 h de refresco? ¿Las mismas para candidatos, o
   una sesión más corta, atada al tiempo para responder el cuestionario?
3. **Acceso de invitado**: ¿se elimina, o queda solo en desarrollo
   (`NODE_ENV=development`) con permisos de solo lectura?
4. **Alta de consultores**: si alguien existe en LDAP pero no en la tabla `consultor`,
   ¿se crea automáticamente en su primer login o se rechaza hasta que un administrador
   lo dé de alta? Propuesta: rechazar (más simple y controlado).
5. **Bloqueo por intentos fallidos**: ¿se agrega (por ejemplo, 5 intentos y 15 min de
   espera) o queda fuera del alcance?

## 10. Fuera de alcance

- Recuperación de contraseña y alta de usuarios en LDAP (se gestionan en el directorio).
- Inicio de sesión con proveedores externos (Google, Microsoft) u OAuth2 completo.
- Autenticación de dos factores.
- Corregir las diferencias entre entidades y esquema que hoy impiden `ddl-auto=validate`
  (por ejemplo `candidato.genero`).
