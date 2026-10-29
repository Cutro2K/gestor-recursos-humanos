-- =============================================================================
-- SCRIPT DE DATOS INICIALES (SEED) - ESQUEMA COMPLETO CON SNAPSHOTS
-- =============================================================================

-- Limpieza integral de todas las tablas y reinicio de secuencias
TRUNCATE TABLE
    registro_ejecucion_proceso,
    registro_auditoria,
    resultado_factor,
    resultado_competencia,
    respuesta_candidato,
    opcion_cuestionario,
    pregunta_cuestionario,
    bloque_cuestionario,
    cuestionario,
    modelo_opcion,
    modelo_pregunta,
    modelo_factor,
    modelo_competencia,
    modelo_evaluacion,
    evaluacion,
    configuracion_cuestionario,
    pregunta_item_opcion,
    pregunta,
    item_opcion,
    opcion_respuesta,
    factor,
    puesto_competencia,
    competencia,
    puesto,
    candidato,
    consultor
RESTART IDENTITY CASCADE;

-- =============================================================================
-- 1. CONSULTOR (2 registros: 1 activo para login y 1 inactivo para prueba)
-- =============================================================================
INSERT INTO consultor (username, password, nombre, apellido, email, activo, fecha_creacion)
VALUES
    ('ADMIN', 'admin123', 'Carlos', 'Gómez', 'carlos.gomez@capitalhumano.com', TRUE, CURRENT_TIMESTAMP),
    ('CONSULTOR2', 'pass456', 'Laura', 'Martínez', 'laura.martinez@capitalhumano.com', FALSE, CURRENT_TIMESTAMP);

-- =============================================================================
-- 2. CANDIDATO (4 registros: 3 activos y 1 dado de baja)
-- =============================================================================
INSERT INTO candidato (nro_candidato, apellido, nombre, tipo_documento, nro_documento, fecha_nacimiento, genero, nacionalidad, email, escolaridad, eliminado)
VALUES
    (1001, 'Pérez', 'Juan', 'DNI', '40123456', '1998-05-14', 'H', 'Argentina', 'juan.perez@mail.com', 'Universitario Completo', FALSE),
    (1002, 'López', 'María', 'DNI', '38987654', '1996-11-22', 'M', 'Argentina', 'maria.lopez@mail.com', 'Universitario En Curso', FALSE),
    (1003, 'Silva', 'Roberto', 'PP', 'AA998877', '1994-03-08', 'H', 'Uruguaya', 'roberto.silva@mail.com', 'Posgrado', FALSE),
    (1004, 'García', 'Ana', 'DNI', '42111222', '2000-07-30', 'M', 'Argentina', 'ana.garcia@mail.com', 'Terciario', TRUE);

-- =============================================================================
-- 3. PUESTO (2 registros)
-- =============================================================================
INSERT INTO puesto (codigo, nombre, descripcion, empresa, eliminado)
VALUES
    ('PUESTO-01', 'Desarrollador Java Senior', 'Desarrollo y mantenimiento de microservicios con Spring Boot y PostgreSQL.', 'Banco Litoral S.A.', FALSE),
    ('PUESTO-02', 'Líder de Proyecto TIC', 'Gestión integral de proyectos tecnológicos bajo estándares PMI.', 'TechSolutions Corp.', FALSE);

-- =============================================================================
-- 4. COMPETENCIA (3 registros: 2 técnicas y 1 actitudinal)
-- =============================================================================
INSERT INTO competencia (codigo, nombre, descripcion, tipo, eliminada)
VALUES
    ('COMP-01', 'Gestión de Proyectos', 'Capacidad para planificar, ejecutar y controlar proyectos de software.', 'TECNICA', FALSE),
    ('COMP-02', 'Ingeniería de Software y Backend', 'Dominio de arquitectura en capas, persistencia JPA y diseño OO.', 'TECNICA', FALSE),
    ('COMP-03', 'Lealtad', 'Compromiso e integridad profesional hacia el equipo y la organización.', 'ACTITUDINAL', FALSE);

-- =============================================================================
-- 5. PUESTO_COMPETENCIA (Ponderaciones mínimas para PUESTO-01)
-- =============================================================================
INSERT INTO puesto_competencia (puesto_id, competencia_id, ponderacion_minima)
VALUES
    (1, 1, 7.00),
    (1, 2, 8.50),
    (1, 3, 6.00);

-- =============================================================================
-- 6. FACTOR (4 registros vinculados a las competencias)
-- =============================================================================
INSERT INTO factor (competencia_id, codigo, nombre, orden, descripcion, eliminado)
VALUES
    (1, 'FAC-01', 'Gestión del alcance', 1, 'Definición de requerimientos y estructura de desglose de trabajo (WBS).', FALSE),
    (1, 'FAC-02', 'Gestión del tiempo', 2, 'Secuenciación de actividades, camino crítico y Valor Ganado.', FALSE),
    (3, 'FAC-03', 'Credibilidad', 1, 'Confiabilidad e integridad en el cumplimiento de responsabilidades.', FALSE),
    (3, 'FAC-04', 'Compromiso', 2, 'Orientación al logro de objetivos colectivos.', FALSE);

-- =============================================================================
-- 7. OPCION_RESPUESTA (2 plantillas de respuestas)
-- =============================================================================
INSERT INTO opcion_respuesta (nombre, descripcion, eliminada)
VALUES
    ('VERDADERO_FALSO', 'Opciones dicotómicas de Verdadero o Falso.', FALSE),
    ('ESCALA_DESEMPENO', 'Escala valorativa de cinco niveles (Malo a Excelente).', FALSE);

-- =============================================================================
-- 8. ITEM_OPCION (Alternativas para cada plantilla)
-- =============================================================================
INSERT INTO item_opcion (opcion_respuesta_id, orden, descripcion)
VALUES
    -- Para VERDADERO_FALSO (id = 1)
    (1, 1, 'Verdadero'),
    (1, 2, 'Falso'),
    -- Para ESCALA_DESEMPENO (id = 2)
    (2, 1, 'Malo'),
    (2, 2, 'Regular'),
    (2, 3, 'Bueno'),
    (2, 4, 'Muy Bueno'),
    (2, 5, 'Excelente');

-- =============================================================================
-- 9. PREGUNTA (Banco de preguntas base)
-- =============================================================================
INSERT INTO pregunta (factor_id, opcion_respuesta_id, nombre, enunciado, descripcion, activa, eliminada)
VALUES
    (1, 1, 'WBS y Alcance', '¿La descomposición de estructura de trabajo (WBS) forma parte de la definición del alcance?', 'Evalúa conceptos base de PMBOK sobre alcance.', TRUE, FALSE),
    (1, 1, 'Línea Base Alcance', '¿Cuándo se define un alcance, debe definirse una línea base de los requerimientos?', 'Evalúa gestión de línea base.', TRUE, FALSE),
    (2, 2, 'Valor Ganado', '¿La metodología de Valor Ganado permite predecir cuánto dinero se gastará en un proyecto?', 'Evalúa control de costos y cronograma.', TRUE, FALSE),
    (3, 2, 'Identificación Profesional', '¿Con cuál de los siguientes niveles calificás tu cumplimiento íntegro de las tareas asignadas?', 'Evalúa credibilidad actitudinal.', TRUE, FALSE);

-- =============================================================================
-- 10. PREGUNTA_ITEM_OPCION (Ponderación de cada ítem según la pregunta)
-- =============================================================================
INSERT INTO pregunta_item_opcion (pregunta_id, item_opcion_id, ponderacion)
VALUES
    -- Pregunta 1 (V/F)
    (1, 1, 10.00),
    (1, 2, 0.00),
    -- Pregunta 2 (V/F)
    (2, 1, 10.00),
    (2, 2, 0.00),
    -- Pregunta 3 (Escala Desempeño)
    (3, 3, 0.00),
    (3, 4, 3.00),
    (3, 5, 6.00),
    (3, 6, 8.50),
    (3, 7, 10.00),
    -- Pregunta 4 (Escala Desempeño)
    (4, 3, 0.00),
    (4, 4, 3.00),
    (4, 5, 6.00),
    (4, 6, 8.50),
    (4, 7, 10.00);

-- =============================================================================
-- 11. CONFIGURACION_CUESTIONARIO
-- =============================================================================
INSERT INTO configuracion_cuestionario (vigencia_desde, vigencia_hasta, instrucciones, cantidad_maxima_accesos, minutos_maximos_activo, minutos_maximos_resolucion, preguntas_por_bloque, activa)
VALUES
    ('2026-01-01 00:00:00', NULL, 'Bienvenido al sistema de evaluación de Capit@l Humano. Lea atentamente cada pregunta. Una vez completado un bloque no podrá retroceder.', 3, 1440, 60, 2, TRUE);

-- =============================================================================
-- 12. EVALUACION
-- =============================================================================
INSERT INTO evaluacion (consultor_id, puesto_id, configuracion_id, fecha_creacion, version_modelo, nombre_archivo_excel)
VALUES
    (1, 1, 1, CURRENT_TIMESTAMP, 'v1.0-2026', '2026-10-29-10-00-DesarrolladorJavaSenior.xls');

-- =============================================================================
-- 13. MODELO_EVALUACION (Snapshot raíz de la evaluación 1)
-- =============================================================================
INSERT INTO modelo_evaluacion (evaluacion_id, fecha_congelamiento, version)
VALUES
    (1, CURRENT_TIMESTAMP, 'v1.0-2026');

-- =============================================================================
-- 14. MODELO_COMPETENCIA (Snapshot de las competencias asociadas al puesto 1)
-- =============================================================================
INSERT INTO modelo_competencia (modelo_evaluacion_id, competencia_origen_id, codigo_snapshot, nombre_snapshot, tipo_snapshot, ponderacion_minima_snapshot)
VALUES
    (1, 1, 'COMP-01', 'Gestión de Proyectos', 'TECNICA', 7.00),
    (1, 3, 'COMP-03', 'Lealtad', 'ACTITUDINAL', 6.00);

-- =============================================================================
-- 15. MODELO_FACTOR (Snapshot de los factores)
-- =============================================================================
INSERT INTO modelo_factor (modelo_competencia_id, factor_origen_id, codigo_snapshot, nombre_snapshot, orden_snapshot)
VALUES
    (1, 1, 'FAC-01', 'Gestión del alcance', 1),
    (1, 2, 'FAC-02', 'Gestión del tiempo', 2),
    (2, 3, 'FAC-03', 'Credibilidad', 1);

-- =============================================================================
-- 16. MODELO_PREGUNTA (Snapshot de las preguntas)
-- =============================================================================
INSERT INTO modelo_pregunta (modelo_factor_id, pregunta_origen_id, nombre_snapshot, enunciado_snapshot, descripcion_snapshot)
VALUES
    (1, 1, 'WBS y Alcance', '¿La descomposición de estructura de trabajo (WBS) forma parte de la definición del alcance?', 'Evalúa conceptos base de PMBOK sobre alcance.'),
    (1, 2, 'Línea Base Alcance', '¿Cuándo se define un alcance, debe definirse una línea base de los requerimientos?', 'Evalúa gestión de línea base.'),
    (2, 3, 'Valor Ganado', '¿La metodología de Valor Ganado permite predecir cuánto dinero se gastará en un proyecto?', 'Evalúa control de costos y cronograma.'),
    (3, 4, 'Identificación Profesional', '¿Con cuál de los siguientes niveles calificás tu cumplimiento íntegro de las tareas asignadas?', 'Evalúa credibilidad actitudinal.');

-- =============================================================================
-- 17. MODELO_OPCION (Snapshot de opciones para las preguntas del modelo)
-- =============================================================================
INSERT INTO modelo_opcion (modelo_pregunta_id, item_opcion_origen_id, orden_snapshot, descripcion_snapshot, ponderacion_snapshot)
VALUES
    -- Pregunta 1
    (1, 1, 1, 'Verdadero', 10.00),
    (1, 2, 2, 'Falso', 0.00),
    -- Pregunta 2
    (2, 1, 1, 'Verdadero', 10.00),
    (2, 2, 2, 'Falso', 0.00),
    -- Pregunta 3
    (3, 3, 1, 'Malo', 0.00),
    (3, 5, 2, 'Bueno', 6.00),
    (3, 7, 3, 'Excelente', 10.00),
    -- Pregunta 4
    (4, 3, 1, 'Malo', 0.00),
    (4, 5, 2, 'Bueno', 6.00),
    (4, 7, 3, 'Excelente', 10.00);

-- =============================================================================
-- 18. CUESTIONARIO (3 cuestionarios para pruebas del CU 01 y CU 26)
-- =============================================================================
INSERT INTO cuestionario (
    evaluacion_id, candidato_id, clave_ingreso, estado,
    fecha_generacion, fecha_inicio, fecha_fin, ultimo_ingreso, fecha_cambio_estado,
    cantidad_accesos, cantidad_maxima_accesos, minutos_maximos_activo, minutos_maximos_resolucion,
    preguntas_por_bloque, puntaje_total
)
VALUES
    -- Cuestionario 1: ACTIVO (Juan Pérez - DNI 40123456 | Clave: A1B2C3D4) -> Login exitoso
    (1, 1, 'A1B2C3D4', 'ACTIVO', CURRENT_TIMESTAMP, NULL, NULL, NULL, CURRENT_TIMESTAMP, 0, 3, 1440, 60, 2, NULL),

    -- Cuestionario 2: EN_PROCESO (María López - DNI 38987654 | Clave: E5F6G7H8) -> Login exitoso (retoma)
    (1, 2, 'E5F6G7H8', 'EN_PROCESO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 3, 1440, 60, 2, NULL),

    -- Cuestionario 3: COMPLETO (Roberto Silva - PP AA998877 | Clave: Z9Y8X7W6) -> Login rechazado (cerrado)
    (1, 3, 'Z9Y8X7W6', 'COMPLETO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 3, 1440, 60, 2, 8.50);

-- =============================================================================
-- 19. BLOQUE_CUESTIONARIO (Para el cuestionario 2 EN_PROCESO)
-- =============================================================================
INSERT INTO bloque_cuestionario (cuestionario_id, numero, completado, fecha_completado)
VALUES
    (2, 1, TRUE, CURRENT_TIMESTAMP),
    (2, 2, FALSE, NULL);

-- =============================================================================
-- 20. PREGUNTA_CUESTIONARIO (Asociadas a los bloques y vinculadas a modelo_pregunta)
-- =============================================================================
INSERT INTO pregunta_cuestionario (bloque_cuestionario_id, modelo_pregunta_id, orden, enunciado_snapshot, respondida)
VALUES
    (1, 1, 1, '¿La descomposición de estructura de trabajo (WBS) forma parte de la definición del alcance?', TRUE),
    (1, 2, 2, '¿Cuándo se define un alcance, debe definirse una línea base de los requerimientos?', TRUE),
    (2, 3, 1, '¿La metodología de Valor Ganado permite predecir cuánto dinero se gastará en un proyecto?', FALSE),
    (2, 4, 2, '¿Con cuál de los siguientes niveles calificás tu cumplimiento íntegro de las tareas asignadas?', FALSE);

-- =============================================================================
-- 21. OPCION_CUESTIONARIO (Vinculadas a pregunta_cuestionario y modelo_opcion)
-- =============================================================================
INSERT INTO opcion_cuestionario (pregunta_cuestionario_id, modelo_opcion_id, orden, descripcion_snapshot, ponderacion_snapshot)
VALUES
    -- Pregunta Cuestionario 1
    (1, 1, 1, 'Verdadero', 10.00),
    (1, 2, 2, 'Falso', 0.00),
    -- Pregunta Cuestionario 2
    (2, 3, 1, 'Verdadero', 10.00),
    (2, 4, 2, 'Falso', 0.00),
    -- Pregunta Cuestionario 3
    (3, 5, 1, 'Malo', 0.00),
    (3, 6, 2, 'Bueno', 6.00),
    (3, 7, 3, 'Excelente', 10.00),
    -- Pregunta Cuestionario 4
    (4, 8, 1, 'Malo', 0.00),
    (4, 9, 2, 'Bueno', 6.00),
    (4, 10, 3, 'Excelente', 10.00);

-- =============================================================================
-- 22. RESPUESTA_CANDIDATO (Respuestas marcadas en el Bloque 1 del Cuestionario 2)
-- =============================================================================
INSERT INTO respuesta_candidato (pregunta_cuestionario_id, opcion_cuestionario_id, fecha_respuesta)
VALUES
    (1, 1, CURRENT_TIMESTAMP),
    (2, 3, CURRENT_TIMESTAMP);

-- =============================================================================
-- 23. REGISTRO_AUDITORIA
-- =============================================================================
INSERT INTO registro_auditoria (consultor_id, fecha_hora, accion, entidad, entidad_id, detalle)
VALUES
    (1, CURRENT_TIMESTAMP, 'ALTA', 'PUESTO', 1, 'Creación inicial del puesto Desarrollador Java Senior');
