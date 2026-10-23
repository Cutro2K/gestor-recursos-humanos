# Sistema de Gestión de Recursos Humanos — TP Desarrollo de Software 2026

Repositorio oficial del Trabajo Práctico de la cátedra **Desarrollo de Software (2026)**. El sistema implementa la gestión de puestos, competencias, factores, preguntas, evaluación de candidatos y emisión de orden de mérito mediante una arquitectura separada en **Backend REST API (Java + Spring Boot)** y **Frontend Web (Next.js + React)**.

---

## Integrantes del Grupo

* **Cutro Tomás Nehuén** — 29752 / tomascutro18@gmail.com
* **Integrante 2** — Legajo / Email
* **Integrante 3** — Legajo / Email
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
* **Node.js:** Versión 20 LTS o superior (junto con `npm`)

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

---

## 4. Casos de Uso y Endpoints de la API REST

A continuación se listan los Casos de Uso implementados y sus respectivos endpoints de ejemplo en el backend (`http://localhost:8080`):

| Caso de Uso | Método HTTP | Endpoint | Descripción |
| :--- | :---: | :--- | :--- |
| **CU 01:** Autenticar usuario | `POST` | `/api/auth/login` | Valida credenciales de usuario del sistema. |
| **CU 06:** Importar candidatos | `POST` | `/api/candidatos/importar` | Importación de candidatos al sistema. |
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
