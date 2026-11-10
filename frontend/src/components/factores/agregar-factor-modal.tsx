"use client";

import { useState } from "react";
import type { FormEvent } from "react";
import { Button, Modal, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { COMPETENCIAS_MOCK, crearFactor } from "@/service/factores";
import styles from "./factores.module.css";

type Errores = {
  competenciaId?: string;
  codigo?: string;
  nombre?: string;
  orden?: string;
  descripcion?: string;
};

const FORM_INICIAL = {
  competenciaId: "",
  codigo: "",
  nombre: "",
  orden: "",
  descripcion: "",
};

function capitalizarPrimero(texto: string): string {
  if (!texto) return "";
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export function AgregarFactorModal({ onCreado }: { onCreado?: () => void }) {
  const [abierto, setAbierto] = useState(false);
  const [formulario, setFormulario] = useState(FORM_INICIAL);
  const [errores, setErrores] = useState<Errores>({});
  const [guardando, setGuardando] = useState(false);
  const [factorRecienCreado, setFactorRecienCreado] = useState<string | null>(null);
  const toast = useToast();

  function limpiarYcerrar() {
    setFormulario(FORM_INICIAL);
    setErrores({});
    setGuardando(false);
    setFactorRecienCreado(null);
    setAbierto(false);
  }

  function cargarOtro() {
    setFormulario(FORM_INICIAL);
    setErrores({});
    setFactorRecienCreado(null);
  }

  function validar(): boolean {
    const nuevosErrores: Errores = {};

    if (!formulario.competenciaId.trim()) {
      nuevosErrores.competenciaId = "Debe seleccionar una competencia.";
    } else if (!/^[1-9]\d*$/.test(formulario.competenciaId.trim())) {
      nuevosErrores.competenciaId = "La competencia seleccionada no es válida.";
    }

    if (!formulario.codigo.trim()) {
      nuevosErrores.codigo = "El código es obligatorio.";
    } else if (formulario.codigo.length > 20) {
      nuevosErrores.codigo = "El código no puede superar los 20 caracteres.";
    }

    if (!formulario.nombre.trim()) {
      nuevosErrores.nombre = "El nombre es obligatorio.";
    } else if (formulario.nombre.length > 50) {
      nuevosErrores.nombre = "El nombre no puede superar los 50 caracteres.";
    }

    if (!formulario.orden.trim()) {
      nuevosErrores.orden = "El número de orden es obligatorio.";
    } else if (!/^[1-9]\d*$/.test(formulario.orden.trim())) {
      nuevosErrores.orden = "El número de orden debe ser un entero positivo.";
    }

    if (!formulario.descripcion.trim()) {
      nuevosErrores.descripcion = "La descripción es obligatoria.";
    }

    setErrores(nuevosErrores);
    return Object.keys(nuevosErrores).length === 0;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!validar()) return;

    setGuardando(true);
    try {
      const nombreNormalizado = capitalizarPrimero(formulario.nombre.trim());
      await crearFactor({
        competenciaId: Number(formulario.competenciaId.trim()),
        codigo: capitalizarPrimero(formulario.codigo.trim()),
        nombre: nombreNormalizado,
        orden: Number(formulario.orden.trim()),
        descripcion: capitalizarPrimero(formulario.descripcion.trim()),
      });

      onCreado?.();
      // CU-017 Paso 3 y 4: Preguntar si desea cargar otro factor
      setFactorRecienCreado(nombreNormalizado);
    } catch (error) {
      const mensaje =
        error instanceof Error ? error.message : "No se pudo registrar el factor.";
      toast.error("No se pudo dar de alta el factor", mensaje);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={(open) => {
        if (!guardando) {
          if (!open) limpiarYcerrar();
          else setAbierto(true);
        }
      }}
      title={factorRecienCreado ? "Factor registrado" : "Dar de alta Factor"}
      description={
        factorRecienCreado
          ? "Confirmación de operación (CU-017)."
          : "Ingresá todos los datos requeridos para dar de alta el nuevo factor."
      }
      trigger={
        <Button type="button" variant="primary">
          Nuevo factor
        </Button>
      }
      size="md"
      closeOnEscape={!guardando}
      showCloseButton={!guardando}
      actions={
        factorRecienCreado ? (
          <>
            <Button type="button" variant="secondary" onClick={limpiarYcerrar}>
              No
            </Button>
            <Button type="button" variant="primary" onClick={cargarOtro}>
              Sí
            </Button>
          </>
        ) : (
          <>
            <Button
              type="button"
              variant="secondary"
              disabled={guardando}
              onClick={limpiarYcerrar}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              form="form-alta-factor"
              loading={guardando}
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Aceptar"}
            </Button>
          </>
        )
      }
    >
      {factorRecienCreado ? (
        <Surface bordered padding="md">
          <p className="text-base font-medium">
            El factor <strong>{factorRecienCreado}</strong> se ha creado correctamente.
          </p>
          <p className={`${styles.muted} mt-2`}>¿Desea cargar otro?</p>
        </Surface>
      ) : (
        <form id="form-alta-factor" onSubmit={handleSubmit} noValidate>
          <Stack gap="md">
            <div className={styles.fieldGroup}>
              <label htmlFor="alta-competencia" className={styles.selectLabel}>
                Competencia *
              </label>
              <select
                id="alta-competencia"
                name="competenciaId"
                className={styles.selectControl}
                value={formulario.competenciaId}
                disabled={guardando}
                aria-invalid={Boolean(errores.competenciaId)}
                onChange={(e) => {
                  const competenciaId = e.target.value;
                  setFormulario((f) => ({ ...f, competenciaId }));
                  if (errores.competenciaId) {
                    setErrores((err) => ({ ...err, competenciaId: undefined }));
                  }
                }}
              >
                <option value="">Seleccionar competencia…</option>
                {COMPETENCIAS_MOCK.map((comp) => (
                  <option key={comp.id} value={String(comp.id)}>
                    {comp.nombre}
                  </option>
                ))}
              </select>
              {errores.competenciaId && (
                <span className={styles.fieldError} role="alert">
                  {errores.competenciaId}
                </span>
              )}
            </div>

            <TextField
              label="Código *"
              name="codigo"
              value={formulario.codigo}
              onChange={(valor) => {
                const codigo = capitalizarPrimero(valor);
                setFormulario((f) => ({ ...f, codigo }));
                if (errores.codigo) setErrores((e) => ({ ...e, codigo: undefined }));
              }}
              maxLength={20}
              error={errores.codigo}
              placeholder="Ej: FAC-01"
              disabled={guardando}
              required
            />

            <TextField
              label="Nombre *"
              name="nombre"
              value={formulario.nombre}
              onChange={(valor) => {
                const nombre = capitalizarPrimero(valor);
                setFormulario((f) => ({ ...f, nombre }));
                if (errores.nombre) setErrores((e) => ({ ...e, nombre: undefined }));
              }}
              maxLength={50}
              error={errores.nombre}
              placeholder="Ej: Capacidad analítica"
              disabled={guardando}
              required
            />

            <TextField
              label="Nro de orden *"
              name="orden"
              type="number"
              value={formulario.orden}
              onChange={(orden) => {
                setFormulario((f) => ({ ...f, orden }));
                if (errores.orden) setErrores((e) => ({ ...e, orden: undefined }));
              }}
              error={errores.orden}
              placeholder="Ej: 1"
              disabled={guardando}
              required
            />

            <TextField
              label="Descripción *"
              name="descripcion"
              value={formulario.descripcion}
              onChange={(valor) => {
                const descripcion = capitalizarPrimero(valor);
                setFormulario((f) => ({ ...f, descripcion }));
                if (errores.descripcion) {
                  setErrores((e) => ({ ...e, descripcion: undefined }));
                }
              }}
              error={errores.descripcion}
              placeholder="Descripción del factor..."
              disabled={guardando}
              required
            />
          </Stack>
        </form>
      )}
    </Modal>
  );
}
