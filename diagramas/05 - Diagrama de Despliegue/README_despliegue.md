# Diagrama de Despliegue - Capit@l Humano (Etapa 6)

Este documento describe la construcción paso a paso del **Diagrama de Despliegue (forma Descriptor)** para el sistema **Capit@l Humano**, correspondiente a la cátedra de **Diseño de Sistemas (2026 - UTN FRSF)**.

Para enriquecer la entrega con ejemplos prácticos y tangibles de la arquitectura física y su implementación real, el diagrama y sus artefactos se encuentran alineados con el desarrollo de software que se implementa en la cátedra de Desarrollo de Software. Puede consultar el repositorio de código y ver en profundidad la implementación en:

🔗 **Repositorio oficial de implementación:** [https://github.com/Cutro2K/gestor-recursos-humanos](https://github.com/Cutro2K/gestor-recursos-humanos)

---

## Archivos generados en este directorio

| Archivo | Formato / Herramienta | Propósito |
| --- | --- | --- |
| `diagrama_despliegue.svg` | Imagen vectorial SVG | Diagrama listo para visualizar o insertar en el informe de la Etapa 6 de Diseño de Sistemas con la estética de la cátedra. |
| `diagrama_despliegue.drawio` | Draw.io (`app.diagrams.net`) | Archivo editable nativo con cajas 3D (`«device»`), entornos anidados (`«executionEnvironment»`) y artefactos (`«artifact»`). |
| `diagrama_despliegue.puml` | PlantUML | Código fuente UML estándar del diagrama de despliegue. |
| `diagrama_despliegue.mmd` | Mermaid | Código fuente en sintaxis Mermaid. |

---

## Construcción paso a paso (según guía de cátedra)

### Paso 1 · Nodos de hardware (`«device»`): ¿En qué equipos corre el sistema?
De acuerdo con el enunciado de Diseño de Sistemas y la arquitectura planteada:
1. **`«device» PC del Consultor`**: Equipo de escritorio en las oficinas de la consultora desde donde opera el actor *Consultor* (gestión de catálogos, evaluación de candidatos, importación/exportación de archivos CSV y emisión de reportes).
2. **`«device» PC del Candidato`**: Equipo del candidato (en su domicilio vía web o en un puesto de la oficina de la consultora) desde donde el actor *Candidato* se autentica con sus credenciales de evaluación (`CU001`) y completa el cuestionario (`CU026`).
3. **`«device» Servidor de aplicaciones`**: Equipo servidor central que aloja el servidor web frontend en **Next.js** (`/frontend`, puerto `3000`) y el backend de lógica de negocio expuesto como **API REST en Java + Spring Boot** (`/api`, puerto `8080`).
4. **`«device» Servidor de BD`**: Equipo servidor dedicado a la persistencia relacional del sistema en **PostgreSQL** (`deso_db`, puerto `5432`).
5. **`«device» Impresora`**: Dispositivo físico de impresión utilizado en los casos de uso `CU027 - Emitir Reporte Comparativo` y `CU028 - Emitir Orden de mérito`.

### Paso 2 · Entornos de ejecución (`«executionEnvironment»`): Runtime, servidor web, motor de BD
Anidados dentro de cada nodo físico:
- En **`PC del Consultor`** y **`PC del Candidato`**:
  - **`«executionEnvironment» Navegador web`**: Ejecuta la interfaz de usuario web del sistema.
- En **`Servidor de aplicaciones`**:
  - **`«executionEnvironment» Node.js (Next.js Server :3000)`**: Entorno de ejecución que sirve la aplicación web frontend desarrollada en Next.js / TypeScript.
  - **`«executionEnvironment» JVM 25 · Spring Boot 4 (Tomcat embebido :8080)`**: Entorno de ejecución Java donde corre el backend REST, los gestores de casos de uso y los controladores.
- En **`Servidor de BD`**:
  - **`«executionEnvironment» PostgreSQL (:5432 - deso_db)`**: Motor de base de datos relacional.

### Paso 3 · Artefactos (`«artifact»`): ¿Qué archivo concreto se instala en cada nodo?
Siguiendo la regla de no colocar clases sueltas sino piezas físicas que manifiestan (`«manifest»`) los componentes lógicos diseñados en las etapas anteriores de Diseño de Sistemas:
- En **`Navegador web`** (`PC del Consultor` y `PC del Candidato`):
  - **`«artifact» frontend-client.js`**: Bundle cliente (Next.js / TypeScript) que materializa las pantallas (`Boundary` en los diagramas de secuencia: `Pantalla Gestionar Preguntas`, `Pantalla Evaluar Candidatos`, `Wizard Cuestionario`, etc.).
- En **`Node.js (Next.js Server :3000)`** (`Servidor de aplicaciones`):
  - **`«artifact» frontend (Next.js App Router)`**: Build de producción de Next.js encargado del renderizado y ruteo del frontend (`src/app/`).
- En **`JVM 25 · Spring Boot 4 (:8080)`** (`Servidor de aplicaciones`):
  - **`«artifact» api.jar (com.deso.api)`**: Ejecutable Java empaquetado que manifiesta los paquetes del Diagrama de Clases (`GestorAutenticacion`, `GestorCandidatos`, `GestorPuestos`, `GestorCompetencias`, `GestorFactores`, `GestorPreguntas`, `GestorOpcionesRespuesta`, `GestorEvaluacion`, `GestorCuestionario`, `GestorReportes`, paquetes de `DTOs`, `Modelo de dominio` y clases `DAO`), además del proceso automático de `CU032 - Finalizar cuestionarios`.
  - **`«artifact» application.properties`**: Archivo físico del repositorio de configuraciones y parámetros del sistema (mencionado en las observaciones de `CU026`).
- En **`PostgreSQL (:5432 - deso_db)`** (`Servidor de BD`):
  - **`«artifact» db/01-schema.sql`**: Script DDL que define las tablas del DER (`CANDIDATO`, `PUESTO`, `COMPETENCIA`, `PUESTO_COMPETENCIA`, `FACTOR`, `OPCION_RESPUESTA`, `ITEM_OPCION`, `PREGUNTA`, etc.).
  - **`«artifact» db/02-data.sql`**: Script DML de datos iniciales (*seed*) para poblar la base de datos y ejecutar los casos de uso.

### Paso 4 · Caminos de comunicación: Protocolos y multiplicidades
- **`PC del Consultor` (`1..*`) — (`1`) `Servidor de aplicaciones`**: Estereotipo **`«HTTP/HTTPS · REST (JSON)»`**. Varios consultores pueden conectarse simultáneamente al servidor para operar el sistema.
- **`PC del Candidato` (`1..*`) — (`1`) `Servidor de aplicaciones`**: Estereotipo **`«HTTP/HTTPS · REST (JSON)»`**. Múltiples candidatos pueden acceder simultáneamente vía web a responder sus cuestionarios (`CU001`, `CU026`).
- **`Node.js (Next.js :3000)` (`1`) — (`1`) `JVM 25 (Spring Boot :8080)`**: Estereotipo **`«HTTP / REST (JSON)»`**. Comunicación entre el frontend Next.js y la API REST de Spring Boot.
- **`Servidor de aplicaciones` (`1`) — (`1`) `Servidor de BD`**: Estereotipo **`«JDBC (TCP/IP :5432)»`**. Los DAOs acceden a PostgreSQL mediante la conexión configurada en `application.properties`.
- **`PC del Consultor` (`1..*`) — (`1`) `Impresora`**: Estereotipo **`«IPP / TCP/IP»`** (impresión de reportes en `CU027` y `CU028`).

### Paso 5 · Sistemas externos y revisión contra Casos de Uso
Se incorporan los tres sistemas externos requeridos por los casos de uso:
1. **`«device» Servidor LDAP de la organización`**: Conectado al `Servidor de aplicaciones` (`1` a `1`) mediante **`«LDAP / LDAPS»`**, utilizado por `GestorAutenticacion` en el **`CU001 - Autenticar usuario`**.
2. **`«device» Servicio de IA` (`«executionEnvironment» API GenAI / LLM`)**: Conectado al `Servidor de aplicaciones` (`1` a `1`) mediante **`«HTTPS / REST (JSON)»`**, invocado en **`CU033 - Generar pregunta con Inteligencia Artificial`** y en la creación asistida de puestos (`CU009`).
3. **`«device» Sistema de Gestión de Candidatos y CV`**: Vinculado con la `PC del Consultor` (`1..*` a `1`) mediante **`«CSV / SMB»`** para intercambio de archivos `.csv` en **`CU006 - Importar Candidatos`** y **`CU007 - Exportar Resultados`**.
