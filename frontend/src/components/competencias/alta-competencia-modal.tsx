"use client";

import { useState } from "react";
import { Button, Modal, RadioGroup, Stack, TextField, useToast } from "react-ui-lib";
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

  function reiniciarFormulario() {
    setCodigo("");
    setNombre("");
    setTipo("TECNICA");
    setDescripcion("");
    setErrores({});
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

    try {
      const creada = await crearCompetencia({
        codigo: codigo.trim().toUpperCase(),
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
        if (error.status === 409) {
          const mensaje = error.message.toLowerCase();
          if (mensaje.includes("código") || mensaje.includes("codigo")) {
            setErrores({ codigo: error.message });
          } else if (mensaje.includes("nombre")) {
            setErrores({ nombre: error.message });
          } else {
            setErrores({ general: error.message });
          }
          toast.error("Conflicto de datos", error.message);
        } else if (error.status === 400) {
          const detalle = error.details as { erroresValidacion?: Record<string, string> } | undefined;
          if (detalle?.erroresValidacion) {
            setErrores(detalle.erroresValidacion);
          } else {
            setErrores({ general: error.message });
          }
          toast.error("Datos inválidos", error.message);
        } else {
          setErrores({ general: error.message });
          toast.error("Error al guardar", error.message);
        }
      } else {
        const mensaje = error instanceof Error ? error.message : "Ocurrió un error inesperado.";
        setErrores({ general: mensaje });
        toast.error("Error", mensaje);
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
      <Stack gap="md" className={styles.formGrid}>
        {errores.general && (
          <div className={styles.fieldError} style={{ padding: "8px 12px", background: "#fee2e2", borderRadius: "6px" }}>
            {errores.general}
          </div>
        )}

        <TextField
          label="Código *"
          placeholder="Ej: COMP-04"
          value={codigo}
          onChange={(val) => {
            setCodigo(val);
            if (errores.codigo) setErrores((prev) => ({ ...prev, codigo: "" }));
          }}
          disabled={guardando}
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
    </Modal>
  );
}
