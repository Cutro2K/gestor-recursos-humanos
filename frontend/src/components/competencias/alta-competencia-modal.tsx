"use client";

import { useState } from "react";
import { Button, Modal, RadioGroup, Stack, TextField, useToast } from "react-ui-lib";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { ApiError } from "@/service/api";
import { crearCompetencia } from "@/service/competencias";
import type { TipoCompetencia } from "@/service/competencias";
import styles from "./competencias.module.css";

type Props = {
  onCreada: () => void;
};

const OPCIONES_TIPO = [
  {
    value: "TECNICA",
    label: "Técnica",
    helpText: "Conocimientos específicos, metodologías y destrezas técnicas del puesto.",
  },
  {
    value: "ACTITUDINAL",
    label: "Actitudinal",
    helpText: "Habilidades interpersonales, valores, adaptabilidad y conducta laboral.",
  },
];

function PlusIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M12 5v14m-7-7h14" />
    </svg>
  );
}

export function AltaCompetenciaModal({ onCreada }: Props) {
  const [abierto, setAbierto] = useState(false);
  const [codigo, setCodigo] = useState("");
  const [nombre, setNombre] = useState("");
  const [tipo, setTipo] = useState<TipoCompetencia>("TECNICA");
  const [descripcion, setDescripcion] = useState("");
  const [guardando, setGuardando] = useState(false);
  const [errores, setErrores] = useState<Record<string, string>>({});
  const toast = useToast();

  const { reportError, feedback, clearError } = useErrorFeedback({
    retrying: guardando,
  });

  function reiniciarFormulario() {
    setCodigo("");
    setNombre("");
    setTipo("TECNICA");
    setDescripcion("");
    setErrores({});
    clearError();
  }

  function handleOpenChange(nuevoEstado: boolean) {
    if (guardando) return;
    setAbierto(nuevoEstado);
    if (!nuevoEstado) {
      reiniciarFormulario();
    }
  }

  function validar(): boolean {
    const nuevosErrores: Record<string, string> = {};

    const codLimpio = codigo.trim();
    if (!codLimpio) {
      nuevosErrores.codigo = "El código es obligatorio.";
    } else if (codLimpio.length > 20) {
      nuevosErrores.codigo = "El código no puede superar los 20 caracteres.";
    }

    const nomLimpio = nombre.trim();
    if (!nomLimpio) {
      nuevosErrores.nombre = "El nombre es obligatorio.";
    } else if (nomLimpio.length > 75) {
      nuevosErrores.nombre = "El nombre no puede superar los 75 caracteres.";
    }

    if (!tipo) {
      nuevosErrores.tipo = "Debe seleccionar el tipo de competencia.";
    }

    if (descripcion.trim().length > 1000) {
      nuevosErrores.descripcion = "La descripción no puede superar los 1000 caracteres.";
    }

    setErrores(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  }

  async function handleGuardar() {
    if (!validar()) return;

    setGuardando(true);
    setErrores({});
    clearError();

    try {
      const creada = await crearCompetencia({
        codigo: codigo.trim(),
        nombre: nombre.trim(),
        tipo,
        descripcion: descripcion.trim() || undefined,
      });

      toast.success(
        "Competencia registrada",
        `Se dio de alta exitosamente la competencia ${creada.codigo} - ${creada.nombre}.`,
      );

      handleOpenChange(false);
      onCreada();
    } catch (error) {
      if (error instanceof ApiError) {
        const detalle = error.details as { erroresValidacion?: Record<string, string> } | undefined;
        if (detalle?.erroresValidacion && Object.keys(detalle.erroresValidacion).length > 0) {
          setErrores(detalle.erroresValidacion);
          toast.error(error.status === 409 ? "Conflicto de datos" : "Datos inválidos", error.message);
        } else {
          reportError(error, {
            title: error.status === 409 ? "Conflicto de datos" : "Error al guardar",
            fallback: error.message,
            retry: () => void handleGuardar(),
          });
        }
      } else {
        reportError(error, {
          title: "Error inesperado",
          fallback: error instanceof Error ? error.message : "Ocurrió un error inesperado al registrar la competencia.",
          retry: () => void handleGuardar(),
        });
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={handleOpenChange}
      title="Dar de alta Competencia"
      description="Registrá una nueva capacidad o habilidad profesional para incluirla en puestos y factores de evaluación (CU 13)."
      trigger={(
        <Button leadingIcon={<PlusIcon />}>
          Nueva competencia
        </Button>
      )}
      className={styles.theme}
      style={{ maxWidth: "38rem" }}
      size="md"
      closeOnEscape={!guardando}
      closeOnOutsideClick={false}
      showCloseButton={!guardando}
      actions={(
        <div className={styles.modalFooter}>
          <Button variant="secondary" disabled={guardando} onClick={() => handleOpenChange(false)}>
            Cancelar
          </Button>
          <Button loading={guardando} onClick={() => void handleGuardar()}>
            {guardando ? "Guardando…" : "Dar de alta"}
          </Button>
        </div>
      )}
    >
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void handleGuardar();
        }}
        noValidate
      >
        <Stack gap="md" className={styles.formGrid}>
          {feedback}

          <TextField
            label="Código *"
            placeholder="Ej: COMP-04"
            value={codigo}
            onChange={(val) => {
              setCodigo(val);
              if (errores.codigo) setErrores((prev) => ({ ...prev, codigo: "" }));
            }}
            disabled={guardando}
            maxLength={20}
            error={errores.codigo}
            helpText="Identificador único de la competencia (hasta 20 caracteres)."
          />

          <TextField
            label="Nombre *"
            placeholder="Ej: Liderazgo y Conducción de Equipos"
            value={nombre}
            onChange={(val) => {
              setNombre(val);
              if (errores.nombre) setErrores((prev) => ({ ...prev, nombre: "" }));
            }}
            disabled={guardando}
            maxLength={75}
            error={errores.nombre}
            helpText="Nombre representativo y único de la competencia (hasta 75 caracteres)."
          />

          <RadioGroup
            label="Tipo de competencia *"
            value={tipo}
            onValueChange={(val) => {
              setTipo(val as TipoCompetencia);
              if (errores.tipo) setErrores((prev) => ({ ...prev, tipo: "" }));
            }}
            options={OPCIONES_TIPO}
            error={errores.tipo}
            disabled={guardando}
            orientation="vertical"
          />

          <div className={styles.textareaField}>
            <label htmlFor="competencia-descripcion" className={styles.textareaLabel}>
              Descripción (opcional)
            </label>
            <textarea
              id="competencia-descripcion"
              className={styles.textarea}
              placeholder="Detalle o alcance de lo que evalúa esta competencia..."
              value={descripcion}
              maxLength={1000}
              onChange={(e) => {
                setDescripcion(e.target.value);
                if (errores.descripcion) setErrores((prev) => ({ ...prev, descripcion: "" }));
              }}
              disabled={guardando}
            />
            <div className={styles.charCount}>
              {descripcion.length} / 1000 caracteres
            </div>
            {errores.descripcion && (
              <div className={styles.fieldError}>{errores.descripcion}</div>
            )}
          </div>
        </Stack>
      </form>
    </Modal>
  );
}
