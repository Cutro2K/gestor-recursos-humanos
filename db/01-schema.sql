-- [PBI-1] Consultor y Candidato
-- =============================================================================
-- TABLA: CONSULTOR
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
-- =============================================================================
CREATE TABLE puesto_competencia (
    puesto_id BIGINT NOT NULL,
    competencia_id BIGINT NOT NULL,
    ponderacion_minima DECIMAL(4, 2) NOT NULL,

    CONSTRAINT pk_puesto_competencia PRIMARY KEY (puesto_id, competencia_id),
    --- El ON DELETE RESTRICT Prohibe la eliminación de las columnas donde se encuentren
    --- los id's relacionados con esta tabla, hasta que la misma sea eliminada primero.
    CONSTRAINT fk_puesto_competencia_puesto FOREIGN KEY (puesto_id)
        REFERENCES puesto(id) ON DELETE RESTRICT, 
    CONSTRAINT fk_puesto_competencia_competencia FOREIGN KEY (competencia_id)
        REFERENCES competencia(id) ON DELETE RESTRICT,
    CONSTRAINT chk_puesto_competencia_ponderacion 
        CHECK (ponderacion_minima >= 0 AND ponderacion_minima <= 10)
);

-- =============================================================================
-- TABLA: FACTOR
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