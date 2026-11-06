# Plan de implementación: CU-01 con LDAP y JWT rotables

> Estado: **diseño acordado, implementación parcial** (actualizado el 2026-11-06).
> Rama de trabajo: `feature/cu01-autenticar-usuario`.
> Diagrama: `diagramas/06 - Diagramas de Secuencia/cu01-autenticacion.html`.

Este plan reemplaza a la propuesta original. Los cambios principales son: LDAP en Docker
en lugar de embebido, refresh JWT con `seq` en lugar de token opaco, una fila por sesión en
lugar de la tabla `token_refresco` con familias, y sesión también para candidatos.

## 1. Estado actual

| Parte | Estado |
| --- | --- |
| Consultores validados contra OpenLDAP | Implementado |
| Límites del CU 01 para consultores (usuario 15, contraseña 20, mayúsculas) | Implementado |
| Candidato: mensaje único, acceso atómico, `INCOMPLETO` al superar accesos | Implementado |
| Sesión, tokens, cookies y Spring Security | Pendiente |
| `proxy.ts` y frontend con cookies de la API | Pendiente |

## 2. Requisitos del enunciado

- **CU 01:** el consultor se valida contra LDAP. El candidato ingresa tipo y número de
  documento y clave, con un único mensaje de error: "Los datos ingresados no son válidos o
  no existe un cuestionario para el Candidato".
- **CU 26:** el candidato puede irse y volver a continuar. Hay un máximo de accesos; si se
  supera, el cuestionario pasa a `incompleto`. El tiempo máximo se mide desde el primer
  ingreso. Los parámetros salen de la configuración del sistema.
- **CU 25:** la clave es aleatoria, de 8 letras y dígitos, y se genera con la evaluación.

El enunciado no define tokens, duración de sesión ni cierre de sesión: esas son decisiones
del grupo, siempre que no rompan lo anterior.

## 3. Arquitectura

```
Navegador ──(cookies HttpOnly)──► API Spring Boot :8080 ──► OpenLDAP (consultores)
    │                                     │
    └── Next.js :3000                     └──► PostgreSQL (consultor, candidato,
        proxy.ts verifica la firma              cuestionario, sesion)
        con la clave pública (JWKS)
```

La API emite y valida los tokens. El frontend nunca los lee desde JavaScript.

## 4. Sesión

### 4.1 Tokens

| | Access token | Refresh token |
| --- | --- | --- |
| Formato | JWT RS256 | JWT RS256 |
| Duración | 10 minutos | Hasta `expira_en` de la sesión |
| Contenido | `sub`, rol, `tipo: "access"`, `exp` | `sesionId`, `seq`, `sub`, `tipo: "refresh"` |
| Cookie | `HttpOnly`, `SameSite=Lax`, `Path=/` | `HttpOnly`, `Path=/api/auth` |
| Verificación | Firma, `exp` y `tipo`, sin tocar la base | Firma, `tipo` y comparación con la fila |

El claim `tipo` se verifica siempre: los dos tokens usan la misma clave, y sin ese control
un refresh token serviría como access token. La cookie de acceso debe durar más que el
`exp` del JWT, hasta la vida de la sesión, para que `proxy.ts` pueda distinguir un token
vencido de una sesión inexistente. En producción, las cookies llevan también `Secure`.

### 4.2 Tabla de sesión

Columnas: `id BIGSERIAL`, `consultor_id` y `cuestionario_id` (ambas nullable, con FK y un
`CHECK` de que exactamente una tenga valor), `inicio`, `expira_en` y `seq`.

| | Consultor | Candidato |
| --- | --- | --- |
| Credenciales | Tabla `consultor` + bind LDAP | Clave, estado y documento del cuestionario |
| Duración | 8 h desde el login | Lo antes entre 2 h (configurable) y el tiempo restante desde `fecha_inicio` |
| Sesiones simultáneas | Sin restricción | Una por cuestionario: un login nuevo reemplaza la anterior |

La duración no se extiende al refrescar.

### 4.3 Flujos

- **Request normal:** Spring Security verifica firma, `exp` y `tipo`.
- **401:** el front hace un único `/api/auth/refresco` compartido y reintenta una vez. Si
  falla, va a `/login`. `/refresco` queda excluido del reintento.
- **`/refresco`:** sesión inexistente → 401. `seq` viejo, sesión vencida o titular no
  habilitado → se borra la fila y 401. Si está bien, rota de forma atómica y emite tokens
  nuevos: `UPDATE ... SET seq = seq + 1 WHERE id = ? AND seq = ?`, verificando las filas
  afectadas. "Habilitado" es consultor activo, o cuestionario abierto y candidato no eliminado.
- **Logout:** `POST /api/auth/logout` borra la fila y limpia las cookies.

Costos aceptados: hasta 10 minutos de acceso residual tras un logout o una baja, re-login
ocasional por la carrera entre pestañas, y una clave robada sirve hasta agotar los accesos.

## 5. Seguridad

- **Guardia:** Spring Security, con la API sin estado y todo bloqueado por defecto. El CORS
  pasa de `WebConfig` a la configuración de seguridad.
- **Firma:** RS256. Clave privada en un archivo PEM fuera del repo, con su ruta en una
  variable de entorno, y un script para que cada integrante genere la suya. Sin rotación de
  claves por ahora.
- **Clave pública:** `GET /api/auth/jwks.json`, que Next consume con `jose`.
- **`proxy.ts`:** control optimista de UX. Verifica solo la firma, no `exp`, porque la cookie
  de refresco no le llega. Sin cookie o con firma inválida va a `/login`; si está vencido
  pero es auténtico, deja pasar y el front refresca ante el 401.
- **CSRF:** token de Spring Security, enviado en un encabezado por `apiRequest` en todo
  `POST`, `PUT` y `DELETE`. La cookie del token CSRF no puede ser `HttpOnly`. `SameSite=Lax`
  suma una segunda capa. En producción, front y API deben compartir dominio.

## 6. Orden de trabajo

1. Tabla `sesion`, entidad y DAO.
2. Claves RSA, emisión de tokens, cookies y endpoints.
3. Spring Security: reglas por rol, CORS y CSRF.
4. Frontend: `api.ts`, `session-provider.tsx`, `proxy.ts` y logout.
5. Control del tiempo máximo del CU 26 y `fecha_inicio`, junto con ese caso de uso.
6. README con variables de entorno y credenciales de prueba.

Cada feature con interfaz se prueba en Chrome, Firefox y Safari antes de darla por funcional.

## 7. Fuera de alcance

- Recuperación de contraseña y alta de usuarios en LDAP.
- Proveedores externos (Google, Microsoft) y autenticación de dos factores.
- Bloqueo por intentos fallidos.
- Rotación de claves de firma.
- Corregir las diferencias entre entidades y esquema que impiden `ddl-auto=validate`.
