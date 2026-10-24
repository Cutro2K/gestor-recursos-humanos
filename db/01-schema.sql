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

