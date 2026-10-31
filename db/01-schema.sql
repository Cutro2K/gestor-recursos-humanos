-- =============================================================================
-- DEFINICIONES: 
-- snapshot: (guardado temporal de estado).
-- ON DELETE RESTRICT: Prohibe la eliminación de las columnas donde se encuentren
-- los id's relacionados con esta tabla, hasta que la misma sea eliminada primero.
-- =============================================================================

-- [PBI-1] Consultor y Candidato
-- =============================================================================
-- TABLA: CONSULTOR
-- Usuarios internos de RRHH habilitados para autenticarse (CU 01), administrar
-- los catálogos de perfiles/preguntas y gestionar evaluaciones de candidatos.
-- =============================================================================
CREATE TABLE consultor (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(30) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_consultor_username UNIQUE (username),
    CONSTRAINT uq_consultor_email UNIQUE (email)
);

-- =============================================================================
-- TABLA: CANDIDATO
-- Postulantes importados o registrados en el sistema (CU 06) que pueden ser
-- asignados a evaluaciones y acceder a resolver sus cuestionarios mediante su clave.
-- =============================================================================
CREATE TABLE candidato (
    id BIGSERIAL PRIMARY KEY,
    nro_candidato BIGINT NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL,
    nro_documento VARCHAR(20) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    genero CHAR(1) NOT NULL,
    nacionalidad VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    escolaridad VARCHAR(50) NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_candidato_nro UNIQUE (nro_candidato),
    CONSTRAINT uq_candidato_documento UNIQUE (tipo_documento, nro_documento),
    CONSTRAINT chk_candidato_genero CHECK (genero IN ('H', 'M')),
    CONSTRAINT chk_candidato_tipo_doc CHECK (tipo_documento IN ('DNI', 'LE', 'LC', 'PP'))
);

-- [PBI-2] Puesto, Competencia, Puesto_Competencia y Factor
-- =============================================================================
-- TABLA: PUESTO
-- Define los cargos o perfiles laborales de las empresas clientes sobre
-- los cuales se estructuran las competencias requeridas para evaluar candidatos.
-- =============================================================================
CREATE TABLE puesto (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(75) NOT NULL,
    descripcion TEXT,
    empresa VARCHAR(100) NOT NULL,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_puesto_codigo UNIQUE (codigo),
    CONSTRAINT uq_puesto_nombre UNIQUE (nombre)
);

-- =============================================================================
-- TABLA: COMPETENCIA
-- Catálogo de habilidades técnicas o actitudinales (CU 13) que componen el perfil
-- de un puesto y se evalúan a través de sus factores asociados.
-- =============================================================================
CREATE TABLE competencia (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(75) NOT NULL,
    descripcion TEXT,
    tipo VARCHAR(20) NOT NULL,
    eliminada BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_competencia_codigo UNIQUE (codigo),
    CONSTRAINT uq_competencia_nombre UNIQUE (nombre),
    CONSTRAINT chk_competencia_tipo CHECK (tipo IN ('TECNICA', 'ACTITUDINAL'))
);

-- =============================================================================
-- TABLA INTERMEDIA: PUESTO_COMPETENCIA
-- Tabla intermedia que vincula cada puesto con las competencias que requiere evaluar,
-- definiendo la ponderación mínima (0 a 10) exigida para aprobar cada una.
-- =============================================================================
CREATE TABLE puesto_competencia (
    puesto_id BIGINT NOT NULL,
    competencia_id BIGINT NOT NULL,
    ponderacion_minima DECIMAL(4, 2) NOT NULL,

    CONSTRAINT pk_puesto_competencia PRIMARY KEY (puesto_id, competencia_id),
    CONSTRAINT fk_puesto_competencia_puesto FOREIGN KEY (puesto_id)
        REFERENCES puesto(id) ON DELETE RESTRICT, 
    CONSTRAINT fk_puesto_competencia_competencia FOREIGN KEY (competencia_id)
        REFERENCES competencia(id) ON DELETE RESTRICT,
    CONSTRAINT chk_puesto_competencia_ponderacion 
        CHECK (ponderacion_minima >= 0 AND ponderacion_minima <= 10)
);

-- =============================================================================
-- TABLA: FACTOR
-- Subdimensiones o aspectos específicos (CU 17) en los que se desglosa una competencia
-- y que agrupan directamente a las preguntas del banco de evaluación.
-- =============================================================================
CREATE TABLE factor (
    id BIGSERIAL PRIMARY KEY,
    competencia_id BIGINT NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    orden INTEGER NOT NULL,
    descripcion TEXT,
    eliminado BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_factor_competencia FOREIGN KEY (competencia_id)
        REFERENCES competencia(id) ON DELETE RESTRICT,
    CONSTRAINT uq_factor_codigo UNIQUE (codigo),
    CONSTRAINT uq_factor_nombre UNIQUE (nombre),
    CONSTRAINT uq_factor_competencia_orden UNIQUE (competencia_id, orden),
    CONSTRAINT chk_factor_orden CHECK (orden > 0)
);

-- [PBI-3] Opciones de Respuesta, Ítems de Opción, Preguntas y Ponderaciones
-- =============================================================================
-- TABLA: OPCION_RESPUESTA
-- Agrupador o plantilla de respuestas reutilizable por múltiples preguntas del banco.
-- =============================================================================
CREATE TABLE opcion_respuesta (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT,
    eliminada BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_opcion_respuesta_nombre UNIQUE (nombre)
);

-- =============================================================================
-- TABLA: ITEM_OPCION
-- Cada una de las alternativas individuales de texto (ej. "Siempre", "A veces", "Nunca")
-- que conforman una opción de respuesta en un orden determinado.
-- =============================================================================
CREATE TABLE item_opcion (
    id BIGSERIAL PRIMARY KEY,
    opcion_respuesta_id BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    descripcion VARCHAR(150) NOT NULL,

    CONSTRAINT fk_item_opcion_opcion_respuesta FOREIGN KEY (opcion_respuesta_id)
        REFERENCES opcion_respuesta(id) ON DELETE RESTRICT,
    CONSTRAINT uq_item_opcion_orden UNIQUE (opcion_respuesta_id, orden),
    CONSTRAINT uq_item_opcion_descripcion UNIQUE (opcion_respuesta_id, descripcion),
    CONSTRAINT chk_item_opcion_orden CHECK (orden > 0)
);

-- =============================================================================
-- TABLA: PREGUNTA
-- Banco de preguntas (CU 20) asociadas a un factor que utilizan opciones
-- de respuesta para evaluar el nivel del candidato en dicho factor.
-- =============================================================================
CREATE TABLE pregunta (
    id BIGSERIAL PRIMARY KEY,
    factor_id BIGINT NOT NULL,
    opcion_respuesta_id BIGINT NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    enunciado VARCHAR(255) NOT NULL,
    descripcion TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    eliminada BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_pregunta_factor FOREIGN KEY (factor_id)
        REFERENCES factor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pregunta_opcion_respuesta FOREIGN KEY (opcion_respuesta_id)
        REFERENCES opcion_respuesta(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pregunta_nombre UNIQUE (factor_id, nombre),
    CONSTRAINT uq_pregunta_enunciado UNIQUE (factor_id, enunciado)
);

-- =============================================================================
-- TABLA INTERMEDIA: PREGUNTA_ITEM_OPCION
-- Asigna el puntaje o ponderación específica (0 a 10) que otorga cada ítem de opción
-- cuando es seleccionado como respuesta a una pregunta determinada.
-- =============================================================================
CREATE TABLE pregunta_item_opcion (
    pregunta_id BIGINT NOT NULL,
    item_opcion_id BIGINT NOT NULL,
    ponderacion DECIMAL(4, 2) NOT NULL,

    CONSTRAINT pk_pregunta_item_opcion PRIMARY KEY (pregunta_id, item_opcion_id),
    CONSTRAINT fk_pregunta_item_opcion_pregunta FOREIGN KEY (pregunta_id)
        REFERENCES pregunta(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pregunta_item_opcion_item FOREIGN KEY (item_opcion_id)
        REFERENCES item_opcion(id) ON DELETE RESTRICT,
    CONSTRAINT chk_pregunta_item_opcion_ponderacion CHECK (ponderacion >= 0)
);

-- [PBI-4] Configuración, Evaluaciones, Cuestionarios, Respuestas y Auditoría
-- =============================================================================
-- TABLA: CONFIGURACION_CUESTIONARIO
-- Almacena los parámetros globales vigentes para los cuestionarios (instrucciones,
-- tiempos máximos de resolución/inactividad, límite de accesos y preguntas por bloque).
-- =============================================================================
CREATE TABLE configuracion_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    vigencia_desde TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vigencia_hasta TIMESTAMP WITHOUT TIME ZONE,
    instrucciones TEXT NOT NULL,
    cantidad_maxima_accesos INTEGER NOT NULL,
    minutos_maximos_activo INTEGER NOT NULL,
    minutos_maximos_resolucion INTEGER NOT NULL,
    preguntas_por_bloque INTEGER NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_config_accesos CHECK (cantidad_maxima_accesos > 0),
    CONSTRAINT chk_config_min_activo CHECK (minutos_maximos_activo > 0),
    CONSTRAINT chk_config_min_resolucion CHECK (minutos_maximos_resolucion > 0),
    CONSTRAINT chk_config_preguntas_bloque CHECK (preguntas_por_bloque > 0)
);

-- =============================================================================
-- TABLA: EVALUACION
-- Representa un proceso de evaluación iniciado por un consultor (CU 25) para cubrir
-- un puesto específico, vinculando la configuración vigente y el lote de candidatos.
-- =============================================================================
CREATE TABLE evaluacion (
    id BIGSERIAL PRIMARY KEY,
    consultor_id BIGINT NOT NULL,
    puesto_id BIGINT NOT NULL,
    configuracion_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version_modelo VARCHAR(30) NOT NULL,
    nombre_archivo_excel VARCHAR(255),
    CONSTRAINT fk_evaluacion_consultor FOREIGN KEY (consultor_id)
        REFERENCES consultor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluacion_puesto FOREIGN KEY (puesto_id)
        REFERENCES puesto(id) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluacion_configuracion FOREIGN KEY (configuracion_id)
        REFERENCES configuracion_cuestionario(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: MODELO_EVALUACION (Snapshot)
-- Cabecera del snapshot de la estructura del puesto al momento de
-- crear la evaluación, aislando el proceso de cambios futuros en los catálogos.
-- =============================================================================
CREATE TABLE modelo_evaluacion (
    id BIGSERIAL PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL,
    fecha_congelamiento TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version VARCHAR(30) NOT NULL,
    CONSTRAINT uq_modelo_evaluacion UNIQUE (evaluacion_id),
    CONSTRAINT fk_modelo_evaluacion_evaluacion FOREIGN KEY (evaluacion_id)
        REFERENCES evaluacion(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: MODELO_COMPETENCIA (Snapshot)
-- Congela los datos y la ponderación mínima exigida de cada competencia asociada al
-- puesto en el instante exacto en que se generó la evaluación.
-- =============================================================================
CREATE TABLE modelo_competencia (
    id BIGSERIAL PRIMARY KEY,
    modelo_evaluacion_id BIGINT NOT NULL,
    competencia_origen_id BIGINT NOT NULL,
    codigo_snapshot VARCHAR(30) NOT NULL,
    nombre_snapshot VARCHAR(75) NOT NULL,
    tipo_snapshot VARCHAR(30) NOT NULL,
    ponderacion_minima_snapshot DECIMAL(4, 2) NOT NULL,
    CONSTRAINT fk_modelo_competencia_modelo FOREIGN KEY (modelo_evaluacion_id)
        REFERENCES modelo_evaluacion(id) ON DELETE RESTRICT,
    CONSTRAINT fk_modelo_competencia_origen FOREIGN KEY (competencia_origen_id)
        REFERENCES competencia(id) ON DELETE RESTRICT,
    CONSTRAINT uq_modelo_competencia UNIQUE (modelo_evaluacion_id, competencia_origen_id)
);

-- =============================================================================
-- TABLA: MODELO_FACTOR (Snapshot)
-- Congela los factores pertenecientes a cada competencia del modelo de evaluación,
-- preservando su código, nombre y orden original para el cálculo posterior.
-- =============================================================================
CREATE TABLE modelo_factor (
    id BIGSERIAL PRIMARY KEY,
    modelo_competencia_id BIGINT NOT NULL,
    factor_origen_id BIGINT NOT NULL,
    codigo_snapshot VARCHAR(30) NOT NULL,
    nombre_snapshot VARCHAR(50) NOT NULL,
    orden_snapshot INTEGER NOT NULL,
    CONSTRAINT fk_modelo_factor_competencia FOREIGN KEY (modelo_competencia_id)
        REFERENCES modelo_competencia(id) ON DELETE RESTRICT,
    CONSTRAINT fk_modelo_factor_origen FOREIGN KEY (factor_origen_id)
        REFERENCES factor(id) ON DELETE RESTRICT,
    CONSTRAINT uq_modelo_factor UNIQUE (modelo_competencia_id, factor_origen_id)
);

-- =============================================================================
-- TABLA: MODELO_PREGUNTA (Snapshot)
-- Congela las preguntas activas de cada factor disponibles al momento de evaluar,
-- sirviendo como universo base para sortear las preguntas de cada candidato.
-- =============================================================================
CREATE TABLE modelo_pregunta (
    id BIGSERIAL PRIMARY KEY,
    modelo_factor_id BIGINT NOT NULL,
    pregunta_origen_id BIGINT NOT NULL,
    nombre_snapshot VARCHAR(50) NOT NULL,
    enunciado_snapshot VARCHAR(255) NOT NULL,
    descripcion_snapshot TEXT,
    CONSTRAINT fk_modelo_pregunta_factor FOREIGN KEY (modelo_factor_id)
        REFERENCES modelo_factor(id) ON DELETE RESTRICT,
    CONSTRAINT fk_modelo_pregunta_origen FOREIGN KEY (pregunta_origen_id)
        REFERENCES pregunta(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: MODELO_OPCION (Snapshot)
-- Congela las opciones de respuesta de cada pregunta del modelo junto con su orden
-- y ponderación exacta, garantizando que el puntaje no varíe si se edita el catálogo.
-- =============================================================================
CREATE TABLE modelo_opcion (
    id BIGSERIAL PRIMARY KEY,
    modelo_pregunta_id BIGINT NOT NULL,
    item_opcion_origen_id BIGINT NOT NULL,
    orden_snapshot INTEGER NOT NULL,
    descripcion_snapshot VARCHAR(150) NOT NULL,
    ponderacion_snapshot DECIMAL(4, 2) NOT NULL,
    CONSTRAINT fk_modelo_opcion_pregunta FOREIGN KEY (modelo_pregunta_id)
        REFERENCES modelo_pregunta(id) ON DELETE RESTRICT,
    CONSTRAINT fk_modelo_opcion_item_origen FOREIGN KEY (item_opcion_origen_id)
        REFERENCES item_opcion(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: CUESTIONARIO
-- Instancia individual de examen asignada a un candidato dentro de una evaluación;
-- gestiona su clave de acceso, ciclo de vida (estados), tiempos, accesos y puntaje final.
-- =============================================================================
CREATE TABLE cuestionario (
    id BIGSERIAL PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL,
    candidato_id BIGINT NOT NULL,
    clave_ingreso CHAR(8) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    fecha_generacion TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio TIMESTAMP WITHOUT TIME ZONE,
    fecha_fin TIMESTAMP WITHOUT TIME ZONE,
    ultimo_ingreso TIMESTAMP WITHOUT TIME ZONE,
    fecha_cambio_estado TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cantidad_accesos INTEGER NOT NULL DEFAULT 0,
    cantidad_maxima_accesos INTEGER NOT NULL,
    minutos_maximos_activo INTEGER NOT NULL,
    minutos_maximos_resolucion INTEGER NOT NULL,
    preguntas_por_bloque INTEGER NOT NULL,
    puntaje_total DECIMAL(6, 2),
    CONSTRAINT uq_cuestionario_evaluacion_candidato UNIQUE (evaluacion_id, candidato_id),
    CONSTRAINT uq_cuestionario_clave UNIQUE (clave_ingreso),
    CONSTRAINT fk_cuestionario_evaluacion FOREIGN KEY (evaluacion_id)
        REFERENCES evaluacion(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cuestionario_candidato FOREIGN KEY (candidato_id)
        REFERENCES candidato(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cuestionario_estado 
        CHECK (estado IN ('ACTIVO', 'EN_PROCESO', 'COMPLETO', 'INCOMPLETO', 'SIN_CONTESTAR'))
);

-- =============================================================================
-- TABLA: BLOQUE_CUESTIONARIO
-- Divide las preguntas sorteadas del cuestionario en páginas o bloques numerados,
-- permitiendo guardar el avance parcial del candidato bloque por bloque (CU 26).
-- =============================================================================
CREATE TABLE bloque_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    cuestionario_id BIGINT NOT NULL,
    numero INTEGER NOT NULL,
    completado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_completado TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_bloque_cuestionario FOREIGN KEY (cuestionario_id)
        REFERENCES cuestionario(id) ON DELETE RESTRICT,
    CONSTRAINT uq_bloque_cuestionario_numero UNIQUE (cuestionario_id, numero)
);

-- =============================================================================
-- TABLA: PREGUNTA_CUESTIONARIO
-- Representa cada pregunta específica sorteada e incluida dentro de un bloque del
-- cuestionario de un candidato, registrando su orden de aparición y si fue respondida.
-- =============================================================================
CREATE TABLE pregunta_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    bloque_cuestionario_id BIGINT NOT NULL,
    modelo_pregunta_id BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    enunciado_snapshot VARCHAR(255) NOT NULL,
    respondida BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pregunta_cuestionario_bloque FOREIGN KEY (bloque_cuestionario_id)
        REFERENCES bloque_cuestionario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pregunta_cuestionario_modelo FOREIGN KEY (modelo_pregunta_id)
        REFERENCES modelo_pregunta(id) ON DELETE RESTRICT,
    CONSTRAINT uq_pregunta_cuestionario_orden UNIQUE (bloque_cuestionario_id, orden)
);

-- =============================================================================
-- TABLA: OPCION_CUESTIONARIO
-- Opciones de respuesta disponibles para una pregunta concreta del cuestionario del
-- candidato, manteniendo la descripción y ponderación congeladas para su corrección.
-- =============================================================================
CREATE TABLE opcion_cuestionario (
    id BIGSERIAL PRIMARY KEY,
    pregunta_cuestionario_id BIGINT NOT NULL,
    modelo_opcion_id BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    descripcion_snapshot VARCHAR(150) NOT NULL,
    ponderacion_snapshot DECIMAL(4, 2) NOT NULL,
    CONSTRAINT fk_opcion_cuestionario_pregunta FOREIGN KEY (pregunta_cuestionario_id)
        REFERENCES pregunta_cuestionario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_opcion_cuestionario_modelo FOREIGN KEY (modelo_opcion_id)
        REFERENCES modelo_opcion(id) ON DELETE RESTRICT,
    CONSTRAINT uq_opcion_cuestionario_orden UNIQUE (pregunta_cuestionario_id, orden)
);

-- =============================================================================
-- TABLA: RESPUESTA_CANDIDATO
-- Registra la opción elegida por el candidato para cada pregunta de su cuestionario
-- junto con la fecha y hora exacta de selección.
-- =============================================================================
CREATE TABLE respuesta_candidato (
    id BIGSERIAL PRIMARY KEY,
    pregunta_cuestionario_id BIGINT NOT NULL,
    opcion_cuestionario_id BIGINT NOT NULL,
    fecha_respuesta TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_respuesta_pregunta UNIQUE (pregunta_cuestionario_id),
    CONSTRAINT fk_respuesta_pregunta FOREIGN KEY (pregunta_cuestionario_id)
        REFERENCES pregunta_cuestionario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_respuesta_opcion FOREIGN KEY (opcion_cuestionario_id)
        REFERENCES opcion_cuestionario(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: RESULTADO_COMPETENCIA
-- Almacena el puntaje final obtenido por el candidato en cada competencia evaluada,
-- utilizado para verificar si alcanza el mínimo del puesto y armar el Orden de Mérito (CU 28).
-- =============================================================================
CREATE TABLE resultado_competencia (
    id BIGSERIAL PRIMARY KEY,
    cuestionario_id BIGINT NOT NULL,
    modelo_competencia_id BIGINT NOT NULL,
    puntaje_obtenido DECIMAL(6, 2) NOT NULL,
    CONSTRAINT fk_resultado_competencia_cuestionario FOREIGN KEY (cuestionario_id)
        REFERENCES cuestionario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_resultado_competencia_modelo FOREIGN KEY (modelo_competencia_id)
        REFERENCES modelo_competencia(id) ON DELETE RESTRICT,
    CONSTRAINT uq_resultado_competencia UNIQUE (cuestionario_id, modelo_competencia_id)
);

-- =============================================================================
-- TABLA: RESULTADO_FACTOR
-- Almacena el puntaje parcial obtenido por el candidato desglosado por cada factor,
-- a partir del cual se calcula el resultado de su competencia.
-- =============================================================================
CREATE TABLE resultado_factor (
    id BIGSERIAL PRIMARY KEY,
    resultado_competencia_id BIGINT NOT NULL,
    modelo_factor_id BIGINT NOT NULL,
    puntaje_obtenido DECIMAL(6, 2) NOT NULL,
    CONSTRAINT fk_resultado_factor_resultado_comp FOREIGN KEY (resultado_competencia_id)
        REFERENCES resultado_competencia(id) ON DELETE RESTRICT,
    CONSTRAINT fk_resultado_factor_modelo FOREIGN KEY (modelo_factor_id)
        REFERENCES modelo_factor(id) ON DELETE RESTRICT,
    CONSTRAINT uq_resultado_factor UNIQUE (resultado_competencia_id, modelo_factor_id)
);

-- =============================================================================
-- TABLA: REGISTRO_AUDITORIA
-- Regístro de seguridad de las acciones de alta, modificación o baja lógica
-- realizadas por los consultores sobre las entidades del sistema.
-- =============================================================================
CREATE TABLE registro_auditoria (
    id BIGSERIAL PRIMARY KEY,
    consultor_id BIGINT NOT NULL,
    fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id BIGINT NOT NULL,
    detalle TEXT,
    CONSTRAINT fk_registro_auditoria_consultor FOREIGN KEY (consultor_id)
        REFERENCES consultor(id) ON DELETE RESTRICT
);

-- =============================================================================
-- TABLA: REGISTRO_EJECUCION_PROCESO 
-- Registro para auditar el inicio y finalización de procesos automáticos en
-- segundo plano (ej. paso automático de cuestionarios vencidos al estado INCOMPLETO).
-- =============================================================================
CREATE TABLE registro_ejecucion_proceso (
    id BIGSERIAL PRIMARY KEY,
    proceso VARCHAR(100) NOT NULL,
    fecha_hora TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion VARCHAR(30) NOT NULL,
    resultado VARCHAR(50),
    detalle TEXT,
    CONSTRAINT chk_proceso_accion CHECK (accion IN ('INICIO', 'FINALIZACION'))
);