# Product Backlog — Gestor de Recursos Humanos (Capit@l Humano)

Este documento contiene el **Product Backlog** completo del proyecto sincronizado 1 a 1 con la numeración de **Jira (`UTNFRSF-1` a `UTNFRSF-19` / `PBI-01` a `PBI-19`)**, estructurado en **Épicas (Epics)**, **Historias de Usuario / Ítems de Backlog (Stories & Tasks)**, **Pre-requisitos bloqueantes** y **Subtareas técnicas**.

---

## Resumen de Épicas (Creadas en Jira)

| Código Épica | Nombre de la Épica en Jira | Ítems Asociados (`UTNFRSF` / `PBI`) |
| :--- | :--- | :--- |
| **EPIC-01** | Infraestructura, Base de Datos y Dominio Base | `PBI-01`, `PBI-02`, `PBI-03` |
| **EPIC-02** | Seguridad, Autenticación y Candidatos | `PBI-04` (CU 01), `PBI-05` (CU 06) |
| **EPIC-03** | Administración de Perfiles y Catálogos | `PBI-06` (CU 13), `PBI-07` (CU 17), `PBI-08` (CU 09) |
| **EPIC-04** | Banco de Preguntas y Opciones de Respuesta | `PBI-09` (CU 24/29), `PBI-10` (CU 20/21), `PBI-11` (CU 22/23), `PBI-19` (CU 33 IA) |
| **EPIC-05** | Evaluación de Candidatos y Cuestionarios | `PBI-12` (CU 25), `PBI-13` (CU 26), `PBI-14` (CU 28) |
| **EPIC-06** | Calidad, Patrones de Diseño, Testing y Entrega | `PBI-15` (Patrones), `PBI-16` (Tests), `PBI-17` (Seed/Docs), `PBI-18` (Swagger/SOLID) |

---

## Mapa Rápido de Dependencias Bloqueantes

```text
PBI-01 (SQL Schema) ──► PBI-02 (Entidades JPA + DAOs) ─┬─► PBI-04 (CU 01 Login) ────────────────────────────┐
                                                       ├─► PBI-05 (CU 06 Importar Candidatos) ──────────────┼─► PBI-12 (CU 25 Evaluar) ──► PBI-13 (CU 26 Cuestionario) ──► PBI-14 (CU 28 Orden Mérito)
PBI-03 (Layout Next.js) ───────────────────────────────┼─► PBI-06 (CU 13 Competencia) ─┬─► PBI-08 (CU 09 Puesto) ┘
                                                       │                               └─► PBI-07 (CU 17 Factor) ──┐
                                                       └─► PBI-09 (CU 24/29 Opciones Resp.) ───────────────────────┴─► PBI-10 (CU 20/21 Preguntas) ─┬─► PBI-11 (CU 22/23 Modif/Baja Pregunta)
                                                                                                                                                    └─► PBI-19 (CU 33 IA - Opcional)
```

---

## Detalle del Product Backlog (Sincronizado con Jira `UTNFRSF-1` a `UTNFRSF-19`)

### EPIC-01: Infraestructura, Base de Datos y Dominio Base

#### `PBI-01` (`UTNFRSF-1`) · Diseño e implementación del Esquema Relacional SQL (`01-schema.sql`)
* **Tipo en Jira:** Task (`core`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `core/esquema-base-de-datos`
* **Pre-requisitos bloqueantes:**
  * *Ninguno (Es el punto de partida del backend y la persistencia).*
* **Descripción:** Traducir el Diagrama Entidad-Relación (DER) al script DDL `db/01-schema.sql` para PostgreSQL (`deso_db`), creando todas las tablas, claves primarias, claves foráneas y restricciones de unicidad.
* **Subtareas para Jira:**
  1. `[DB]` Crear tablas de Usuarios/Consultores y Candidatos.
  2. `[DB]` Crear tablas de Puestos, Competencias, Puesto_Competencia y Factores.
  3. `[DB]` Crear tablas de Opciones de Respuesta, Ítems de Opción y Preguntas.
  4. `[DB]` Crear tablas de Evaluaciones, Cuestionarios, Bloques, Respuestas y Auditoría.
* **Criterios de Aceptación:** El script `psql -U postgres -d deso_db -f db/01-schema.sql` se ejecuta sin errores de sintaxis.

#### `PBI-02` (`UTNFRSF-2`) · Mapeo de Entidades del Modelo de Dominio (JPA) e Interfaces DAO Base
* **Tipo en Jira:** Task (`core`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `core/entidades-y-daos-base`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`)** (Requiere tener definido el esquema relacional y nombres de tablas/columnas en `01-schema.sql`).
* **Descripción:** Implementar las clases del paquete `com.deso.api.model` respetando encapsulamiento, herencia y polimorfismo del Diagrama de Clases, junto con la estructura del patrón DAO en `com.deso.api.dao`.
* **Subtareas para Jira:**
  1. `[Backend]` Crear entidades JPA (`@Entity`) para el dominio de catálogos (`Puesto`, `Competencia`, `Factor`, `Pregunta`, `OpcionRespuesta`, `ItemOpcion`).
  2. `[Backend]` Crear entidades JPA para el dominio de evaluación (`Candidato`, `Evaluacion`, `Cuestionario`, `PreguntaCuestionario`, `RespuestaCandidato`, `RegistroAuditoria`).
  3. `[Backend]` Configurar arquitectura de manejo global de excepciones (`@RestControllerAdvice`) y CORS para conectar con Next.js (`localhost:3000`).
* **Criterios de Aceptación:** El proyecto Spring Boot levanta con `ddl-auto=update` / `validate` sin errores de mapeo de Hibernate.

#### `PBI-03` (`UTNFRSF-3`) · Estructura de Navegación Base y Layout en Next.js
* **Tipo en Jira:** Task (`core`)
* **Prioridad:** High | **Story Points:** 3 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `core/layout-y-cliente-api-frontend`
* **Pre-requisitos bloqueantes:**
  * *Ninguno (Puede desarrollarse en paralelo con `PBI-01` y `PBI-02`).*
* **Descripción:** Crear el layout principal en Next.js (`src/app/layout.tsx`), barra de navegación, componentes visuales reutilizables (botones, inputs con validación, alertas de error/éxito) y el cliente HTTP base en `src/service/` para comunicarse con `http://localhost:8080/api`.
* **Criterios de Aceptación:** El frontend levanta en `localhost:3000` y permite navegar entre las secciones principales con feedback visual consistente.

---

### EPIC-02: Seguridad, Autenticación y Candidatos

#### `PBI-04` (`UTNFRSF-4`) · [CU 01] Autenticar Usuario (Consultor y Candidato)
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `feature/cu01-autenticar-usuario`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`)** (Tablas de Usuarios y Candidatos creadas en la BD).
  * 🔒 **`PBI-02` (`UTNFRSF-2`)** (Entidades y DAOs base en Spring Boot).
  * 🔒 **`PBI-03` (`UTNFRSF-3`)** (Layout y servicio cliente HTTP en Next.js).
* **Descripción:** *Como* usuario del sistema (Consultor o Candidato), *quiero* iniciar sesión ingresando mis credenciales (usuario/documento y contraseña/clave de evaluación) *para* acceder a las funcionalidades correspondientes a mi rol.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `UsuarioDAO`, `CandidatoDAO` y `GestorAutenticacion` (validación de credenciales de Consultor y credenciales de acceso a cuestionario para Candidato).
  2. `[Backend - Controller]` Exponer endpoint `POST /api/auth/login` con DTOs de entrada/salida y manejo de excepciones (`CredencialesInvalidasException`).
  3. `[Frontend - Next.js]` Crear pantalla en `src/app/login/page.tsx` con validación de campos vacíos, mensajes de error claros y redirección según rol (Panel de Consultor vs. Cuestionario de Candidato).
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Valida campos obligatorios y muestra feedback claro si las credenciales son incorrectas.

#### `PBI-05` (`UTNFRSF-5`) · [CU 06] Importar Candidatos
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** High | **Story Points:** 5 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `feature/cu06-importar-candidatos`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`)** (Tabla `CANDIDATO` creada en la BD).
  * 🔒 **`PBI-02` (`UTNFRSF-2`)** (Entidad `Candidato` y estructura DAO en Spring Boot).
  * 🔒 **`PBI-03` (`UTNFRSF-3`)** (Layout y cliente HTTP en Next.js).
* **Descripción:** *Como* Consultor, *quiero* importar un lote de candidatos al sistema (desde archivo CSV / formulario de importación) *para* darlos de alta o actualizarlos y poder evaluarlos posteriormente.
* **Subtareas para Jira:**
  1. `[Backend - DAO]` Implementar `CandidatoDAO` para búsqueda por documento/email y persistencia por lote.
  2. `[Backend - Service]` Implementar lógica de parseo, validación de formato y reglas de negocio en `GestorCandidatos` (usando Streams y Lambdas para filtrar/procesar registros).
  3. `[Backend - Controller]` Exponer endpoints `POST /api/candidatos/importar` y `GET /api/candidatos` (necesario para listarlos en CU 25).
  4. `[Frontend - Next.js]` Crear pantalla `src/app/candidatos/importar/page.tsx` con carga de archivo, previsualización/resumen de candidatos importados y reporte de errores por fila.
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Los candidatos importados quedan persistidos en PostgreSQL y disponibles para el CU 25.

---

### EPIC-03: Administración de Perfiles y Catálogos

#### `PBI-06` (`UTNFRSF-6`) · [CU 13] Dar de alta Competencia (+ Listado de Competencias)
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** High | **Story Points:** 3 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `feature/cu13-alta-competencia`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`)** (Tabla `COMPETENCIA` en la BD).
  * 🔒 **`PBI-02` (`UTNFRSF-2`)** (Entidad `Competencia` y estructura DAO en Spring Boot).
  * 🔒 **`PBI-03` (`UTNFRSF-3`)** (Layout y cliente HTTP en Next.js).
* **Descripción:** *Como* Consultor, *quiero* registrar una nueva competencia con su código, nombre y descripción *para* poder asignarla a los puestos y asociarle factores de evaluación.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `CompetenciaDAO` y `GestorCompetencias` (validando que el código o nombre no se repita).
  2. `[Backend - Controller]` Exponer `POST /api/competencias` y `GET /api/competencias`.
  3. `[Frontend - Next.js]` Crear vista/modal de alta y listado de competencias (`src/app/competencias/page.tsx`).
* **Criterios de Aceptación:** No permite crear dos competencias con el mismo código/nombre y devuelve errores HTTP semánticos (`400`/`409`).

#### `PBI-07` (`UTNFRSF-7`) · [CU 17] Dar de alta Factor (+ Listado por Competencia)
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** High | **Story Points:** 3 | **Sprint sugerido:** Sprint 1
* **Rama sugerida:** `feature/cu17-alta-factor`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`) y `PBI-02` (`UTNFRSF-2`)** (Tabla y entidad `Factor`).
  * 🔒 **`PBI-06` (`UTNFRSF-6` - CU 13: Dar de alta Competencia)** — *Bloqueante directo: todo Factor debe asociarse obligatoriamente a una Competencia existente.*
* **Descripción:** *Como* Consultor, *quiero* dar de alta un factor asociado a una competencia existente (indicando código, nombre, descripción y orden) *para* poder clasificar las preguntas de evaluación.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `FactorDAO` y `GestorFactores` (validando existencia de la competencia padre y unicidad).
  2. `[Backend - Controller]` Exponer `POST /api/factores` y `GET /api/factores` (con filtro por `competenciaId`).
  3. `[Frontend - Next.js]` Crear vista/modal de alta de factor (`src/app/factores/page.tsx`).
* **Criterios de Aceptación:** El factor queda vinculado a su competencia en la base de datos y disponible para asociar preguntas en el CU 21.

#### `PBI-08` (`UTNFRSF-8`) · [CU 09] Dar de alta Puesto (y Gestión/Listado de Puestos)
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 2
* **Rama sugerida:** `feature/cu09-alta-puesto`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`), `PBI-02` (`UTNFRSF-2`) y `PBI-03` (`UTNFRSF-3`)** (Tablas/entidades `Puesto` y `PuestoCompetencia`, y estructura frontend).
  * 🔒 **`PBI-06` (`UTNFRSF-6` - CU 13: Dar de alta Competencia)** — *Bloqueante directo: para dar de alta un Puesto se requiere listar y seleccionar Competencias existentes asignándoles su ponderación mínima.*
* **Descripción:** *Como* Consultor, *quiero* dar de alta un nuevo Puesto (código, nombre, descripción, empresa y lista de competencias con su ponderación mínima requerida) *para* definir el perfil contra el cual se evaluará a los candidatos.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `PuestoDAO` y `GestorPuestos` (validando código/nombre único y ponderaciones válidas de cada competencia asociada).
  2. `[Backend - Controller]` Exponer `POST /api/puestos` y `GET /api/puestos`.
  3. `[Frontend - Next.js]` Crear pantalla `src/app/puestos/nuevo/page.tsx` con selección dinámica de competencias, carga de puntajes/ponderaciones y validaciones en tiempo real.
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Persiste el puesto junto con sus relaciones `PuestoCompetencia` de manera transaccional (`@Transactional`).

---

### EPIC-04: Banco de Preguntas y Opciones de Respuesta

#### `PBI-09` (`UTNFRSF-9`) · [CU 24 y CU 29] Gestionar Opciones de Respuesta y Dar de alta Opción de Respuesta
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 2
* **Rama sugerida:** `feature/cu24-cu29-opciones-respuesta`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`), `PBI-02` (`UTNFRSF-2`) y `PBI-03` (`UTNFRSF-3`)** (Tablas y entidades `OpcionRespuesta` e `ItemOpcion`, y layout base en Next.js).
* **Descripción:** *Como* Consultor, *quiero* buscar/gestionar las opciones de respuesta existentes (CU 24) y dar de alta nuevas opciones de respuesta con sus ítems y orden de visualización (CU 29) *para* utilizarlas al crear preguntas del cuestionario.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `OpcionRespuestaDAO` y `GestorOpcionesRespuesta` (búsqueda con filtros y alta de `OpcionRespuesta` con su lista de `ItemOpcion`).
  2. `[Backend - Controller]` Exponer `GET /api/opciones-respuesta` y `POST /api/opciones-respuesta`.
  3. `[Frontend - Next.js]` Crear pantallas `src/app/opciones-respuesta/page.tsx` (CU 24) y `src/app/opciones-respuesta/nueva/page.tsx` (CU 29) permitiendo agregar/quitar ítems dinámicamente.
* **Criterios de Aceptación:**
  * Ambos CUs (24 y 29) son ejecutables desde Next.js (Requisito 9).
  * Valida que toda opción de respuesta tenga nombre único y al menos los ítems requeridos.

#### `PBI-10` (`UTNFRSF-10`) · [CU 20 y CU 21] Gestionar Preguntas y Dar de alta Pregunta
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 8 | **Sprint sugerido:** Sprint 2
* **Rama sugerida:** `feature/cu20-cu21-gestionar-alta-pregunta`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-06` (`UTNFRSF-6` - CU 13: Competencias)** y **`PBI-07` (`UTNFRSF-7` - CU 17: Factores)** — *Bloqueantes directos: toda pregunta debe clasificarse seleccionando su Competencia y Factor.*
  * 🔒 **`PBI-09` (`UTNFRSF-9` - CU 24 y CU 29: Opciones de Respuesta)** — *Bloqueante directo: toda pregunta requiere elegir una Opción de Respuesta existente y asignarle ponderaciones a sus ítems.*
* **Descripción:** *Como* Consultor, *quiero* consultar y filtrar el listado de preguntas por competencia/factor/nombre (CU 20) y dar de alta una nueva pregunta asignándole su factor, opción de respuesta y ponderaciones por cada ítem (CU 21) *para* nutrir el banco de preguntas del sistema.
* **Subtareas para Jira:**
  1. `[Backend - DAO/Service]` Implementar `PreguntaDAO` y `GestorPreguntas` con filtros dinámicos (usando Streams/Predicates o consultas JPA) y alta de pregunta con sus ponderaciones por ítem.
  2. `[Backend - Controller]` Exponer `GET /api/preguntas` y `POST /api/preguntas`.
  3. `[Frontend - Next.js]` Crear pantalla de gestión `src/app/preguntas/page.tsx` (CU 20) con filtros de búsqueda y tabla paginada, y pantalla `src/app/preguntas/nueva/page.tsx` (CU 21) con selectores en cascada (Competencia $\rightarrow$ Factor) y asignación de puntajes a cada ítem de respuesta.
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Al seleccionar una Opción de Respuesta en el alta de pregunta, se despliegan sus ítems para ingresar la ponderación de cada uno.

#### `PBI-11` (`UTNFRSF-11`) · [CU 22 y CU 23] Modificar Pregunta y Dar de baja Pregunta
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** High | **Story Points:** 5 | **Sprint sugerido:** Sprint 2
* **Rama sugerida:** `feature/cu22-cu23-modificar-baja-pregunta`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-10` (`UTNFRSF-10` - CU 20 y CU 21: Gestionar y Dar de alta Pregunta)** — *Bloqueante directo: se parte de la grilla de gestión de preguntas (CU 20) y de las preguntas dadas de alta (CU 21) para poder editarlas o darlas de baja.*
* **Descripción:** *Como* Consultor, *quiero* modificar los datos y ponderaciones de una pregunta existente (CU 22) o darla de baja lógica (CU 23) desde la grilla de gestión *para* mantener actualizado el banco de preguntas sin perder trazabilidad histórica.
* **Subtareas para Jira:**
  1. `[Backend - Service/DAO]` Implementar actualización y baja lógica (marcando estado inactivo/eliminado y registrando auditoría si corresponde) en `GestorPreguntas`.
  2. `[Backend - Controller]` Exponer `GET /api/preguntas/{id}`, `PUT /api/preguntas/{id}` y `DELETE /api/preguntas/{id}`.
  3. `[Frontend - Next.js]` Crear pantalla de edición `src/app/preguntas/[id]/editar/page.tsx` (CU 22) y modal de confirmación de baja en la grilla de preguntas (CU 23).
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Las preguntas dadas de baja ya no se sortean en futuros cuestionarios.

---

### EPIC-05: Evaluación de Candidatos y Cuestionarios

#### `PBI-12` (`UTNFRSF-12`) · [CU 25] Evaluar Candidatos (Generación de Evaluación y Cuestionarios)
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 8 | **Sprint sugerido:** Sprint 3
* **Rama sugerida:** `feature/cu25-evaluar-candidatos`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-05` (`UTNFRSF-5` - CU 06: Importar Candidatos)** — *Se necesitan candidatos dados de alta en el sistema para seleccionarlos.*
  * 🔒 **`PBI-08` (`UTNFRSF-8` - CU 09: Dar de alta Puesto)** — *Se necesita un puesto con sus competencias y factores asociados.*
  * 🔒 **`PBI-10` (`UTNFRSF-10` - CU 21: Dar de alta Pregunta)** — *Se necesitan preguntas activas cargadas en cada factor del puesto para poder sortear y armar los bloques del cuestionario.*
* **Descripción:** *Como* Consultor, *quiero* seleccionar un Puesto a evaluar y un conjunto de Candidatos activos *para* generar sus cuestionarios personalizados (sorteando preguntas válidas por cada factor de las competencias del puesto) y emitir sus claves de acceso.
* **Subtareas para Jira:**
  1. `[Backend - Service]` Implementar en `GestorEvaluacion` la validación de que el puesto tenga suficientes preguntas activas por factor, la generación aleatoria de `Cuestionario` / `BloqueCuestionario`, la creación de claves de acceso y la copia instantánea (*snapshot*) del modelo evaluado.
  2. `[Backend - Controller]` Exponer `POST /api/evaluaciones` devolviendo el resumen y listado de candidatos con sus credenciales generadas.
  3. `[Frontend - Next.js]` Crear pantalla `src/app/evaluaciones/nueva/page.tsx` con selector de puesto, selección múltiple de candidatos, validaciones y descarga/visualización de credenciales generadas.
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Si un factor no cumple el mínimo de preguntas requeridas para armar el cuestionario, informa el error claramente al Consultor.

#### `PBI-13` (`UTNFRSF-13`) · [CU 26] Completar Cuestionario
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 8 | **Sprint sugerido:** Sprint 3
* **Rama sugerida:** `feature/cu26-completar-cuestionario`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-04` (`UTNFRSF-4` - CU 01: Autenticar Usuario - Rol Candidato)** — *El candidato debe autenticarse con su documento y clave de evaluación.*
  * 🔒 **`PBI-12` (`UTNFRSF-12` - CU 25: Evaluar Candidatos)** — *Bloqueante directo: debe existir un Cuestionario en estado Activo/En Proceso generado previamente para ese candidato.*
* **Descripción:** *Como* Candidato autenticado con clave de evaluación, *quiero* visualizar las instrucciones, avanzar bloque por bloque respondiendo las preguntas de mi cuestionario y finalizarlo *para* que el sistema calcule automáticamente mis puntajes por factor y competencia.
* **Subtareas para Jira:**
  1. `[Backend - Service]` Implementar en `GestorCuestionario` el ciclo de estados del cuestionario (*Activo*, *En Proceso*, *Completo*, *Sin Contestar*), el guardado de respuestas por bloque, el control de tiempo máximo y el cálculo final de puntajes por factor y competencia.
  2. `[Backend - Controller]` Exponer `GET /api/cuestionarios/actual`, `POST /api/cuestionarios/{id}/bloque` y `POST /api/cuestionarios/{id}/finalizar`.
  3. `[Frontend - Next.js]` Crear wizard interactivo en `src/app/cuestionario/page.tsx` con indicador de progreso por bloques, validación de preguntas sin responder y pantalla de confirmación final.
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Al finalizar el último bloque, el cuestionario pasa a estado *Completo* y quedan persistidos los `ResultadoFactor` y `ResultadoCompetencia`.

#### `PBI-14` (`UTNFRSF-14`) · [CU 28] Emitir Orden de Mérito
* **Tipo en Jira:** Story (`feature`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Sprint 3
* **Rama sugerida:** `feature/cu28-emitir-orden-merito`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-08` (`UTNFRSF-8` - CU 09: Alta de Puesto)**.
  * 🔒 **`PBI-12` (`UTNFRSF-12` - CU 25: Evaluar Candidatos)** y **`PBI-13` (`UTNFRSF-13` - CU 26: Completar Cuestionario)** — *Bloqueantes directos: requiere evaluaciones y cuestionarios finalizados con sus puntajes calculados para armar el ranking.*
* **Descripción:** *Como* Consultor, *quiero* seleccionar un Puesto/Evaluación y visualizar el Orden de Mérito de los candidatos evaluados (clasificados entre quienes aprobaron todas las competencias mínimas y quienes no, ordenados por puntaje ponderado descendente) *para* tomar decisiones de selección.
* **Subtareas para Jira:**
  1. `[Backend - Service]` Implementar el cálculo de ranking y clasificación en `GestorReportes` / `GestorEvaluacion` utilizando `Streams`, `Comparator` y expresiones Lambda.
  2. `[Backend - Controller]` Exponer `GET /api/evaluaciones/{id}/orden-merito`.
  3. `[Frontend - Next.js]` Crear pantalla `src/app/orden-merito/page.tsx` mostrando las tablas de candidatos que cumplen y no cumplen los mínimos, puntajes detallados y botón de impresión (`window.print()` / exportación).
* **Criterios de Aceptación:**
  * Ejecutable desde Next.js (Requisito 9).
  * Ordena correctamente los candidatos según su porcentaje/puntaje de adecuación al puesto.

---

### EPIC-06: Calidad, Patrones de Diseño, Testing y Entrega (Obligatorios + Valorados)

#### `PBI-15` (`UTNFRSF-15`) · Implementación y verificación de los 4 Patrones de Diseño (Requisito 7)
* **Tipo en Jira:** Task (`core`)
* **Prioridad:** Highest | **Story Points:** 5 | **Sprint sugerido:** Transversal (Sprints 2 y 3)
* **Rama sugerida:** `core/patrones-de-diseno`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-02` (`UTNFRSF-2`)** (Entidades y estructura base del backend). Se integra sobre los servicios de **`PBI-08`**, **`PBI-12`**, **`PBI-13`** y **`PBI-14`**.
* **Descripción:** Asegurar la implementación explícita de al menos **4 patrones de diseño** (estructurales, creacionales o de comportamiento) además del patrón DAO (Requisito 5).
* **Propuesta de 4 Patrones ideales para el TP:**
  1. **Strategy (Comportamiento):** Para las estrategias de cálculo de puntaje / orden de mérito o algoritmos de selección de preguntas del cuestionario.
  2. **State (Comportamiento):** Para gestionar el ciclo de vida y transiciones válidas de estados del `Cuestionario` (*Activo* $\rightarrow$ *EnProceso* $\rightarrow$ *Completo* / *Incompleto* / *SinContestar*).
  3. **Builder (Creacional):** Para la construcción paso a paso de objetos complejos como `Cuestionario` (con sus bloques y preguntas sorteadas) o `Puesto` con sus competencias.
  4. **Adapter / Facade (Estructural):** `Facade` en la capa de servicios para coordinar múltiples subsistemas al evaluar candidatos (CU 25), o `Adapter` para la importación de candidatos CSV (CU 06) / integración con LDAP (CU 01) / IA (CU 33).

#### `PBI-16` (`UTNFRSF-16`) · Suite de Tests Unitarios con JUnit 5 y Mockito (Requisito 10: $\ge$ 80% en 3 Servicios)
* **Tipo en Jira:** Task (`test`)
* **Prioridad:** Highest | **Story Points:** 8 | **Sprint sugerido:** Sprint 4
* **Rama sugerida:** `core/tests-unitarios-servicios`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-08` (`UTNFRSF-8`)**, **`PBI-10` (`UTNFRSF-10`)** y **`PBI-12` (`UTNFRSF-12`)** (Tener implementadas las 3 clases de la capa de servicio `service/` que serán testeadas, ej. `GestorPuestos`, `GestorPreguntas` y `GestorEvaluacion`).
* **Descripción:** Implementar pruebas unitarias con **JUnit 5** y **Mockito** (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) logrando como mínimo un **80% de cobertura de código en 3 clases diferentes de la capa de servicio**.
* **Subtareas para Jira:**
  1. `[Test]` Configurar plugin **JaCoCo** en `api/pom.xml` para medir y generar el reporte visual de porcentaje de cobertura (`target/site/jacoco/index.html`).
  2. `[Test]` Escribir tests unitarios de casos felices y excepciones para `GestorPuestos` ($\ge 80\%$).
  3. `[Test]` Escribir tests unitarios de casos felices y excepciones para `GestorPreguntas` ($\ge 80\%$).
  4. `[Test]` Escribir tests unitarios de casos felices y excepciones para `GestorEvaluacion` / `GestorCuestionario` ($\ge 80\%$).

#### `PBI-17` (`UTNFRSF-17`) · Script de Datos de Prueba (`02-data.sql`) y Documentación Final (`README.md`)
* **Tipo en Jira:** Task (`core`)
* **Prioridad:** Highest | **Story Points:** 3 | **Sprint sugerido:** Sprint 4
* **Rama sugerida:** `core/seed-datos-y-documentacion`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-01` (`UTNFRSF-1`)** (Esquema SQL definitivo) y **`PBI-04` a `PBI-14` (`UTNFRSF-4` a `UTNFRSF-14`)** (Todos los casos de uso obligatorios implementados para que los datos de prueba cubran todas las tablas y endpoints).
* **Descripción:** Poblar `db/02-data.sql` con usuarios, candidatos, puestos, competencias, factores, opciones de respuesta, preguntas activas suficientes para sortear cuestionarios y evaluaciones completadas para probar inmediatamente el Orden de Mérito (CU 28). Actualizar `README.md` con todos los endpoints finales y ejemplos de JSON.

#### `PBI-18` (`UTNFRSF-18`) · [VALORADO / BONUS] Buenas Prácticas Avanzadas (Swagger, Javadoc, SOLID y Java Funcional)
* **Tipo en Jira:** Task (`core` - Bonus)
* **Prioridad:** Medium | **Story Points:** 3 | **Sprint sugerido:** Sprint 4
* **Rama sugerida:** `core/swagger-javadoc-refinamiento`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-02` (`UTNFRSF-2`)** y **`PBI-04` a `PBI-14` (`UTNFRSF-4` a `UTNFRSF-14`)** (Controladores, servicios y DAOs implementados para documentarlos con Swagger y Javadoc).
* **Descripción:** Aplicar los ítems valorados en la sección *"Consideraciones"* del enunciado para sumar puntaje extra en la corrección:
  1. **Swagger / OpenAPI:** Agregar `springdoc-openapi-starter-webmvc-ui` en `pom.xml` y anotar los controladores con `@Operation` y `@Tag`.
  2. **Javadoc:** Documentar las clases y métodos principales de la capa `service` y `dao` con comentarios `/** ... */`.
  3. **Java Moderno:** Verificar el uso intensivo de `Optional<T>` en los DAOs, `Generics` en el DAO base (`GenericDAO<T, ID>`), interfaces funcionales (`Predicate`, `Function`, `Supplier`) y clases de `java.time` (`LocalDateTime`, `Duration`).
  4. **Principios SOLID:** Inyección de dependencias por constructor mediante interfaces en todos los servicios y controladores.

---

### Ítem Opcional en EPIC-04 (`UTNFRSF-19` / `PBI-19`)

#### `PBI-19` (`UTNFRSF-19`) · [CU 33] [OPCIONAL] Generar Pregunta con Inteligencia Artificial
* **Tipo en Jira:** Story (`feature` - Opcional, asociada a `EPIC-04`)
* **Prioridad:** Low | **Story Points:** 5 | **Sprint sugerido:** Sprint 4 (Si hay tiempo disponible)
* **Rama sugerida:** `feature/cu33-generar-pregunta-ia`
* **Pre-requisitos bloqueantes:**
  * 🔒 **`PBI-06` (`UTNFRSF-6` - CU 13)** y **`PBI-07` (`UTNFRSF-7` - CU 17)** (Competencias y Factores).
  * 🔒 **`PBI-10` (`UTNFRSF-10` - CU 21: Dar de alta Pregunta)** — *Bloqueante directo: el CU 33 es una extensión del formulario de Alta de Pregunta.*
* **Descripción:** *Como* Consultor, *quiero* solicitar sugerencias automáticas de texto de pregunta y descripción a partir de una competencia y factor seleccionados mediante una API de IA generativa *para* agilizar la carga de preguntas en el CU 21.
* **Subtareas para Jira:**
  1. `[Backend - Service]` Implementar cliente HTTP hacia API de LLM (ej. Gemini API / OpenAI) aplicando el **Patrón Adapter** o **Strategy** para desacoplar el proveedor de IA.
  2. `[Backend - Controller]` Exponer `POST /api/preguntas/generar-ia`.
  3. `[Frontend - Next.js]` Agregar botón *"Sugerir con IA"* en el formulario de Alta de Pregunta (`CU 21`) que autocompleta los campos sugeridos.
