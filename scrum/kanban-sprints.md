# Estructura Kanban y Planificación de Sprints — Gestor de Recursos Humanos

Este documento define las columnas del **Tablero Kanban en Jira**, las reglas de transición (**Definition of Ready** y **Definition of Done**) y la distribución de las historias del Product Backlog en **4 Sprints** sincronizadas con la numeración de Jira (`UTNFRSF-1` a `UTNFRSF-19` / `PBI-01` a `PBI-19`) hasta la fecha límite de entrega (**13/12/2026**).

---

## 1. Columnas del Tablero Kanban (Configuradas en Jira)

| # | Columna en Jira | Significado / Estado de la Tarea | Regla para mover a esta columna |
| :---: | :--- | :--- | :--- |
| **1** | **TO DO (Tareas por hacer)** | Historias y subtareas comprometidas para el Sprint actual que aún nadie empezó. | La tarea fue seleccionada en la *Sprint Planning*. |
| **2** | **IN PROGRESS (En Progreso)** | Un integrante se asignó la tarea, creó su rama (`feature/...` o `core/...`) y está programando. | Sus pre-requisitos bloqueantes están listos y se ejecutó `git checkout -b <rama>`. |
| **3** | **IN REVIEW (En Revisión / PR)** | El código está terminado localmente, actualizado con la rama base, pusheado y tiene un **Pull Request (PR)** abierto en GitHub. | El PR está creado y esperando que otro compañero revise el código. |
| **4** | **TESTING / QA (En Prueba)** | El PR fue revisado y se está validando que el Caso de Uso funcione de punta a punta (Next.js $\leftrightarrow$ Spring Boot $\leftrightarrow$ PostgreSQL). | Compila sin errores, sin warnings de ESLint y cumple validaciones de UI. |
| **5** | **DONE (Finalizado)** | El Pull Request fue **mergeado** a la rama principal, la rama temporal se eliminó y la funcionalidad está lista. | Merge completado en GitHub. |

---

## 2. Criterios de Calidad del Equipo

### Definition of Ready (DoR) — *¿Cuándo una historia puede entrar en progreso?*
* Tiene claramente identificado qué Caso de Uso cubre y quién la va a desarrollar.
* **Sus pre-requisitos bloqueantes (`PBI` / `UTNFRSF` previos) ya están en `DONE` o integrados.**
* Tiene definido el nombre de su rama según *Conventional Branch* (ej. `feature/cu09-alta-puesto`).

### Definition of Done (DoD) — *¿Cuándo una historia pasa a la columna DONE?*
* **Backend:** Endpoint REST funcionando en Spring Boot (`controller`), lógica de negocio independiente en `service`, acceso a datos mediante `dao` y manejo de excepciones adecuado.
* **Frontend:** Pantalla en Next.js funcionando en `localhost:3000`, con validación de formularios, manejo de errores y mensajes de feedback al usuario (Requisito 9).
* **Git:** Commits siguiendo *Conventional Commits* (`feat:`, `fix:`, `test:`, `docs:`) y Pull Request aprobado y mergeado sin conflictos.

---

## 3. Planificación y Tablero de Seguimiento por Sprint

### 🏁 SPRINT 1: Base de Datos, Dominio, Autenticación y Catálogos Base
* **Objetivo del Sprint (Sprint Goal):** Dejar la base de datos PostgreSQL operativa, las entidades mapeadas en Spring Boot, el layout en Next.js y funcionando los CUs iniciales (Login, Importar Candidatos, Competencias y Factores).

| Clave Jira / PBI | Funcionalidad / Historia | Pre-requisitos Bloqueantes | Puntos | Rama Git | Estado Kanban | Asignado a |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **`UTNFRSF-1` (`PBI-01`)** | Esquema Relacional SQL (`db/01-schema.sql`) | *Ninguno* | 5 | `core/esquema-base-de-datos` | `[ ] TO DO` | - |
| **`UTNFRSF-2` (`PBI-02`)** | Entidades JPA (`model`) e Interfaces DAO base | `PBI-01` | 5 | `core/entidades-y-daos-base` | `[ ] TO DO` | - |
| **`UTNFRSF-3` (`PBI-03`)** | Layout base Next.js y cliente HTTP (`src/service`) | *Ninguno* | 3 | `core/layout-y-cliente-api-frontend` | `[ ] TO DO` | - |
| **`UTNFRSF-4` (`PBI-04`)** | **[CU 01]** Autenticar usuario (Login Web + API) | `PBI-01`, `PBI-02`, `PBI-03` | 5 | `feature/cu01-autenticar-usuario` | `[ ] TO DO` | - |
| **`UTNFRSF-5` (`PBI-05`)** | **[CU 06]** Importar candidatos (Web + API) | `PBI-01`, `PBI-02`, `PBI-03` | 5 | `feature/cu06-importar-candidatos` | `[ ] TO DO` | - |
| **`UTNFRSF-6` (`PBI-06`)** | **[CU 13]** Dar de alta competencia (+ Listado) | `PBI-01`, `PBI-02`, `PBI-03` | 3 | `feature/cu13-alta-competencia` | `[ ] TO DO` | - |
| **`UTNFRSF-7` (`PBI-07`)** | **[CU 17]** Dar de alta factor (+ Listado por Competencia) | `PBI-01`, `PBI-02`, `PBI-06 (CU 13)` | 3 | `feature/cu17-alta-factor` | `[ ] TO DO` | - |

---

### 🏁 SPRINT 2: Gestión de Puestos, Opciones de Respuesta y Banco de Preguntas
* **Objetivo del Sprint (Sprint Goal):** Completar todo el módulo de administración de perfiles y el ABM completo de preguntas y opciones de respuesta desde Next.js.

| Clave Jira / PBI | Funcionalidad / Historia | Pre-requisitos Bloqueantes | Puntos | Rama Git | Estado Kanban | Asignado a |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **`UTNFRSF-8` (`PBI-08`)** | **[CU 09]** Dar de alta puesto (y Listado de Puestos) | `PBI-01..03`, `PBI-06 (CU 13)` | 5 | `feature/cu09-alta-puesto` | `[ ] TO DO` | - |
| **`UTNFRSF-9` (`PBI-09`)** | **[CU 24 y CU 29]** Gestionar y dar de alta opciones de respuesta | `PBI-01`, `PBI-02`, `PBI-03` | 5 | `feature/cu24-cu29-opciones-respuesta` | `[ ] TO DO` | - |
| **`UTNFRSF-10` (`PBI-10`)** | **[CU 20 y CU 21]** Gestionar y dar de alta pregunta | `PBI-06 (CU 13)`, `PBI-07 (CU 17)`, `PBI-09 (CU 24/29)` | 8 | `feature/cu20-cu21-gestionar-alta-pregunta` | `[ ] TO DO` | - |
| **`UTNFRSF-11` (`PBI-11`)** | **[CU 22 y CU 23]** Modificar y dar de baja pregunta | `PBI-10 (CU 20 y CU 21)` | 5 | `feature/cu22-cu23-modificar-baja-pregunta` | `[ ] TO DO` | - |

---

### 🏁 SPRINT 3: Flujo Core de Evaluación, Cuestionario y Orden de Mérito
* **Objetivo del Sprint (Sprint Goal):** Conectar el flujo principal del sistema: que el Consultor pueda evaluar candidatos para un puesto, que el Candidato pueda completar su cuestionario y que se emita el Orden de Mérito.

| Clave Jira / PBI | Funcionalidad / Historia | Pre-requisitos Bloqueantes | Puntos | Rama Git | Estado Kanban | Asignado a |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **`UTNFRSF-12` (`PBI-12`)** | **[CU 25]** Evaluar candidatos (Generación de Cuestionarios) | `PBI-05 (CU 06)`, `PBI-08 (CU 09)`, `PBI-10 (CU 21)` | 8 | `feature/cu25-evaluar-candidatos` | `[ ] TO DO` | - |
| **`UTNFRSF-13` (`PBI-13`)** | **[CU 26]** Completar cuestionario (Wizard Candidato) | `PBI-04 (CU 01)`, `PBI-12 (CU 25)` | 8 | `feature/cu26-completar-cuestionario` | `[ ] TO DO` | - |
| **`UTNFRSF-14` (`PBI-14`)** | **[CU 28]** Emitir orden de mérito (Ranking y vista) | `PBI-08 (CU 09)`, `PBI-12 (CU 25)`, `PBI-13 (CU 26)` | 5 | `feature/cu28-emitir-orden-merito` | `[ ] TO DO` | - |
| **`UTNFRSF-15` (`PBI-15`)** | Implementación y verificación de los **4 Patrones de Diseño** | `PBI-02`, `PBI-08`, `PBI-12..14` | 5 | `core/patrones-de-diseno` | `[ ] TO DO` | - |

---

### 🏁 SPRINT 4: Testing Unitario (80%), Datos Seed, Opcionales y Cierre de Entrega
* **Objetivo del Sprint (Sprint Goal):** Asegurar el 80% de cobertura en 3 servicios con JUnit y Mockito, poblar el script `02-data.sql`, incorporar mejoras valoradas (Swagger, Javadoc, CU 33 IA) y cerrar la entrega antes del 13/12.

| Clave Jira / PBI | Funcionalidad / Historia | Pre-requisitos Bloqueantes | Puntos | Rama Git | Estado Kanban | Asignado a |
| :--- | :--- | :--- | :---: | :--- | :--- | :--- |
| **`UTNFRSF-16` (`PBI-16`)** | **Tests Unitarios (JUnit 5 + Mockito):** $\ge 80\%$ en 3 Servicios | `PBI-08`, `PBI-10`, `PBI-12` (3 servicios listos) | 8 | `core/tests-unitarios-servicios` | `[ ] TO DO` | - |
| **`UTNFRSF-17` (`PBI-17`)** | Script `db/02-data.sql` (Seed completo) y `README.md` final | `PBI-01`, `PBI-04` a `PBI-14` | 3 | `core/seed-datos-y-documentacion` | `[ ] TO DO` | - |
| **`UTNFRSF-18` (`PBI-18`)** | *[VALORADO]* Swagger OpenAPI, Javadoc, SOLID y Java Funcional | `PBI-02`, `PBI-04` a `PBI-14` | 3 | `core/swagger-javadoc-refinamiento` | `[ ] TO DO` | - |
| **`UTNFRSF-19` (`PBI-19`)** | **[CU 33] [OPCIONAL]** Generar pregunta con Inteligencia Artificial | `PBI-06`, `PBI-07`, `PBI-10 (CU 21)` | 5 | `feature/cu33-generar-pregunta-ia` | `[ ] TO DO` | - |
