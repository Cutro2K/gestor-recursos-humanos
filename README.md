# Sistema de Gestión de Recursos Humanos — TP Desarrollo de Software 2026

Repositorio oficial del Trabajo Práctico de la cátedra **Desarrollo de Software (2026)**. El sistema implementa la gestión de puestos, competencias, factores, preguntas, evaluación de candidatos y emisión de orden de mérito mediante una arquitectura separada en **Backend REST API (Java + Spring Boot)** y **Frontend Web (Next.js + React)**.

---

## Integrantes del Grupo

* **Cutro Tomás Nehuén** — 29752 / tomascutro18@gmail.com
* **Gonzalez Bustamante Joaquin** — 29834 / joaquin200589@gmail.com
* **Yoris Bertschi Thiago** — 29976 / thiagoyorisbertschi@gmail.com
* **Integrante 4** — Legajo / Email

---

## Estructura del Repositorio

```text
gestor-recursos-humanos/
├── api/                 # Backend REST API en Java + Spring Boot (Puerto 8080)
├── frontend/            # Aplicación Web en Next.js + TypeScript (Puerto 3000)
├── db/                  # Scripts SQL de creación de esquema y carga de datos (seed)
│   ├── 01-schema.sql    # Creación de tablas y relaciones
│   └── 02-data.sql      # Datos de prueba para todos los Casos de Uso
├── diagramas/           # Diagramas de Clases, Secuencia y Entidad-Relación
└── git-conventions/     # Guías de flujo de trabajo (Conventional Commits y Branches)
```

---

## Requisitos Previos

Asegúrese de tener instaladas las siguientes herramientas antes de levantar el proyecto:

* **Java JDK:** Versión 25 (o superior compatible con la configuración de `api/pom.xml`)
* **Maven:** Incluido mediante el wrapper `./mvnw` dentro de `/api` (no requiere instalación global)
* **PostgreSQL:** Versión 14 o superior (corriendo en el puerto `5432`)
* **Node.js:** Versión 22.12 o superior (junto con `npm`); se recomienda Node.js 24 para el frontend y ReactUILib.

---

## 1. Configuración de la Base de Datos (PostgreSQL)

1. Asegúrese de que el servicio de PostgreSQL esté activo y en ejecución.
2. Ingrese a la consola de PostgreSQL (`psql`) o a un cliente gráfico (pgAdmin / DBeaver) y cree la base de datos `deso_db`:

   ```sql
   CREATE DATABASE deso_db;
   ```


3. Ejecute el script de estructura de tablas (`01-schema.sql`) y el script de datos iniciales / *seed* (`02-data.sql`) ubicados en la carpeta `/db`:

   ```bash
   psql -U postgres -h localhost -d deso_db -f db/01-schema.sql
   psql -U postgres -h localhost -d deso_db -f db/02-data.sql
   ```

---

## 2. Configuración y Ejecución del Backend (`/api`)

### Aviso importante sobre Variables de Entorno

El archivo [`api/src/main/resources/application.properties`](api/src/main/resources/application.properties) está configurado utilizando variables de entorno con **valores por defecto**:

| Variable de Entorno | Descripción | Valor por Defecto (si no se define) |
| :--- | :--- | :--- |
| `DB_URL` | URL JDBC de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/deso_db` |
| `DB_USERNAME` | Usuario del motor PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña del usuario PostgreSQL | `postgres` |

> **Nota:** Si su instalación local de PostgreSQL utiliza el usuario `postgres` con contraseña `postgres` en el puerto `5432`, **no necesita configurar ninguna variable de entorno**; la aplicación funcionará automáticamente con los valores por defecto.  
> Si utiliza credenciales personalizadas en su máquina, defina las variables de entorno antes de iniciar el servidor (sin modificar `application.properties`):
> ```bash
> export DB_USERNAME=mi_usuario
> export DB_PASSWORD=mi_contraseña
> ```

### Levantar el servidor Backend

Desde una terminal, sitúese en la carpeta `api/` y ejecute el wrapper de Maven:

```bash
cd api
./mvnw spring-boot:run
```
*(En Windows: `mvnw.cmd spring-boot:run`)*

* El servidor quedará escuchando en: **`http://localhost:8080`**

### Ejecutar las Pruebas Unitarias (JUnit 5 + Mockito)

Para correr la suite de tests unitarios de la capa de servicio:

```bash
cd api
./mvnw test
```

---

## 3. Configuración y Ejecución del Frontend (`/frontend`)

1. Abra una nueva terminal y sitúese en la carpeta `frontend/`:

   ```bash
   cd frontend
   ```

2. Instale las dependencias de Node.js (solo necesario la primera vez o cuando cambie `package.json`):

   ```bash
   npm install
   ```

3. *(Opcional)* Si el backend no corre en la URL por defecto (`http://localhost:8080/api`), puede crear un archivo `.env.local` dentro de `frontend/` definiendo:

   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080/api
   ```

4. Inicie el servidor de desarrollo de Next.js:

   ```bash
   npm run dev
   ```

* La aplicación web estará disponible en el navegador en: **`http://localhost:3000`**

### Layout y navegación base (PBI-03)

El inicio presenta los accesos al proceso de evaluación y los catálogos. La barra
lateral permite navegar a candidatos, puestos, competencias, factores, preguntas,
opciones de respuesta, evaluaciones, cuestionarios y orden de mérito. En pantallas
pequeñas se despliega con el botón de navegación. Cada sección tiene una ruta
propia y una página base; las operaciones de negocio corresponden a los siguientes
PBIs y todavía no están implementadas. La navegación no aplica permisos por rol:
la autenticación corresponde al PBI-04.

- `src/components/layout/`: estructura compartida y páginas base de sección.
- `src/components/layout/app-shell.module.css`: posición de la navegación y
  distribución de las áreas en escritorio y móvil; no redefine controles.
- `src/components/ui/`: encabezados, enlaces de sección, iconos y mensajes de feedback.
- `src/config/sections.ts`: nombres, rutas, descripciones y relaciones entre secciones.
- `src/app/loading.tsx`, `error.tsx` y `not-found.tsx`: estados comunes de navegación.
- `src/service/api.ts`: cliente HTTP con `fetch`, URL configurable mediante
  `NEXT_PUBLIC_API_URL` y valor por defecto `http://localhost:8080/api`.

Ejemplo para un futuro servicio de competencias:

```ts
import { apiRequest } from "@/service/api";

type Competencia = { id: number; nombre: string };
const competencias = await apiRequest<Competencia[]>("/competencias");
// POST: apiRequest<Competencia>("/competencias", { method: "POST", json: datos });
// Para archivos: { method: "POST", body: formData }, sin Content-Type manual.
// ApiError expone message, status y details; status 0 indica fallo de conexión.
// Las respuestas vacías (por ejemplo, HTTP 204) devuelven undefined.
```

Desde `frontend/`, ejecutá `npm test` para verificar JSON, errores HTTP, respuestas
vacías, carga de archivos y cancelación sin necesitar el backend; `npm run lint`
y `npm run build` validan el frontend. Para probar la API real, Spring Boot debe
estar iniciado y permitir CORS desde el origen del frontend (configuración prevista
en PBI-02).

### Componentes de interfaz (ReactUILib)

El frontend instala `react-ui-lib` desde `frontend/react-ui-lib-0.1.0.tgz`;
`npm install` dentro de `frontend/` también instala sus dependencias. El archivo
`.tgz` debe acompañar al proyecto para que la instalación funcione en otras máquinas.

`src/app/layout.tsx` importa el CSS compilado de la biblioteca antes de los estilos
de la aplicación. `src/app/providers.tsx` es un componente de cliente que configura
`UIProvider` con el tema y movimiento del sistema, y `ToastProvider` para las
notificaciones. Un botón temporal de sol/luna junto al avatar permite alternar
entre **Claro** y **Oscuro** para previsualizar los temas. Inicialmente se respeta
la preferencia del sistema si no hay una elección guardada. La preferencia se guarda en
`localStorage` con la clave `capital-humano-theme` y se sincroniza entre pestañas.
Si el almacenamiento está bloqueado, el cambio funciona durante la sesión de la
pestaña. El botón también está disponible en el encabezado móvil. Los componentes y
sus portales comparten el tema mediante `src/components/theme/theme-provider.tsx`.
El layout permanece como componente de servidor y monta
`AppShell`, que conserva la navegación compartida.

Los controles conservan la apariencia predeterminada de ReactUILib: `Sidebar`
para navegación, `Button` para el cambio de tema, `Surface` para tarjetas y paneles,
`Stack` para agrupación y espaciado. La marca junto al título usa el SVG
`frontend/public/brand/gestor-rrhh.svg`, diseñado en
[Figma](https://www.figma.com/design/yuMkPEz9GT1FyiyOu2vrka?node-id=2-5)
como símbolo de personas y crecimiento, con fondo transparente.
Los iconos SVG de entrada y salida del menú de cuenta también están diseñados en
[Figma](https://www.figma.com/design/yuMkPEz9GT1FyiyOu2vrka?node-id=7-26) y se
renderizan con `currentColor` en `src/components/ui/icon.tsx`. La opción
**Cerrar sesión** usa la variante `destructive` de la biblioteca, con texto e
icono blancos. Las acciones de sesión muestran avisos hasta conectar la autenticación.
Los mensajes en línea se componen con `Surface` y `Stack`, ya que la
biblioteca no ofrece un componente de alerta en línea. Los enlaces usan Next.js
`Link`; las grillas y la tipografía usan utilidades de Tailwind y los tokens
`--ui-*` de la biblioteca. `globals.css` sólo importa Tailwind, aplica la fuente
y sincroniza el esquema de color del navegador. Evitá redefinir bordes, sombras,
radios, colores o animaciones internos de los componentes de la biblioteca.

La página **`http://localhost:3000/ui`** permite probar botones, validación de un
campo, notificaciones y una ventana modal, sin necesitar el backend ni guardar
datos. Los componentes que usen estado, eventos o `useToast()` deben llevar
`"use client"` al principio de su archivo. Importá los controles desde
`react-ui-lib`, por ejemplo: `import { Button, TextField } from "react-ui-lib"`.

Para actualizar la biblioteca, copiá el nuevo `.tgz` a `frontend/`, ejecutá
`npm install ./react-ui-lib-<version>.tgz` desde esa carpeta y verificá
`npm run lint` y `npm run build`. Versioná el paquete, `package.json` y
`package-lock.json` juntos. Los estilos de la biblioteca cargan la fuente
Sansation desde Google Fonts; si no está disponible, se usa una fuente del sistema.

---

## 4. Casos de Uso y Endpoints de la API REST

A continuación se listan los Casos de Uso implementados y sus respectivos endpoints de ejemplo en el backend (`http://localhost:8080`):

| Caso de Uso | Método HTTP | Endpoint | Descripción |
| :--- | :---: | :--- | :--- |
| **CU 01:** Autenticar usuario | `POST` | `/api/auth/login` | Valida credenciales de usuario del sistema. |
| **CU 06:** Previsualizar candidatos | `POST` | `/api/candidatos/previsualizar` | Recibe el CSV multipart y clasifica las filas sin guardar cambios. |
| **CU 06:** Importar candidatos | `POST` | `/api/candidatos/importar` | Confirma el CSV multipart, importa filas válidas e informa rechazos. |
| **CU 25:** Listar candidatos | `GET` | `/api/candidatos` | Lista candidatos activos con filtros y paginación por cursor. |
| **CU 09:** Dar de alta puesto | `POST` | `/api/puestos` | Registra un nuevo puesto con sus características. |
| **CU 13:** Dar de alta competencia | `POST` | `/api/competencias` | Registra una nueva competencia. |
| **CU 17:** Dar de alta factor | `POST` | `/api/factores` | Registra un nuevo factor asociado a una competencia. |
| **CU 20:** Gestionar preguntas | `GET` | `/api/preguntas` | Lista y filtra las preguntas registradas. |
| **CU 21:** Dar de alta pregunta | `POST` | `/api/preguntas` | Crea una nueva pregunta para un factor. |
| **CU 22:** Modificar pregunta | `PUT` | `/api/preguntas/{id}` | Actualiza los datos de una pregunta existente. |
| **CU 23:** Dar de baja pregunta | `DELETE` | `/api/preguntas/{id}` | Realiza la baja de una pregunta. |
| **CU 24:** Gestionar opciones de respuestas | `GET` | `/api/opciones-respuesta` | Lista las opciones de respuesta disponibles. |
| **CU 29:** Dar de alta opción de respuesta | `POST` | `/api/opciones-respuesta` | Crea un nuevo conjunto de opciones de respuesta. |
| **CU 25:** Evaluar candidatos | `POST` | `/api/evaluaciones` | Inicia la evaluación y genera cuestionarios para candidatos. |
| **CU 26:** Completar cuestionario | `POST` | `/api/cuestionarios/{id}/respuestas` | Registra las respuestas de un candidato en su cuestionario. |
| **CU 28:** Emitir orden de mérito | `GET` | `/api/evaluaciones/{puestoId}/orden-merito` | Calcula y emite el orden de mérito de los candidatos evaluados. |

*(Nota: Las rutas exactas de los endpoints se irán actualizando a medida que se completen los controladores de cada Caso de Uso).*

El contrato de los endpoints de candidatos y ejemplos con `curl` están en
[api/docs/cu06-csv.md](api/docs/cu06-csv.md#endpoints-http).
