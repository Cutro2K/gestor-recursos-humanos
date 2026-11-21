"use client";

import { SyntheticEvent, useEffect, useState } from "react";
import { Button, Modal, Stack, TextField, useToast } from "react-ui-lib";
import {
  listarCompetenciasDisponibles,
  modificarFactor,
  obtenerFactorPorId,
} from "@/service/factores";
import type { CompetenciaResumen, Factor } from "@/service/factores";
import styles from "./factores.module.css";

type Errores = {
  competenciaId?: string;
  nombre?: string;
  orden?: string;
  descripcion?: string;
};

type ModificarFactorModalProps = {
  factor: Factor;
  onModificado?: () => void;
};

function capitalizarPrimero(texto: string): string {
  if (!texto) return "";
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

export function ModificarFactorModal({ factor, onModificado }: ModificarFactorModalProps) {
  const [abierto, setAbierto] = useState(false);
  const [competencias, setCompetencias] = useState<CompetenciaResumen[]>([]);
  const [cargandoDatos, setCargandoDatos] = useState(false);
  const [formulario, setFormulario] = useState({
    competenciaId: factor.competenciaId ? String(factor.competenciaId) : "",
    codigo: factor.codigo ?? "",
    nombre: factor.nombre ?? "",
    orden: String(factor.orden ?? ""),
    descripcion: factor.descripcion ?? "",
  });
  const [errores, setErrores] = useState<Errores>({});
  const [guardando, setGuardando] = useState(false);
  const toast = useToast();

  useEffect(() => {
    if (!abierto) return;
    const controller = new AbortController();

    const promesaCompetencias = listarCompetenciasDisponibles(controller.signal);
    const promesaDetalle =
      typeof factor.id === "number"
        ? obtenerFactorPorId(factor.id, controller.signal).catch(() => null)
        : Promise.resolve(null);

    void Promise.all([promesaCompetencias, promesaDetalle])
      .then(([listaCompetencias, detalle]) => {
        if (controller.signal.aborted) return;
        setCompetencias(listaCompetencias);

        if (detalle) {
          setFormulario({
            competenciaId: detalle.competenciaId ? String(detalle.competenciaId) : "",
            codigo: capitalizarPrimero(detalle.codigo ?? ""),
            nombre: capitalizarPrimero(detalle.nombre ?? ""),
            orden: String(detalle.orden ?? ""),
            descripcion: capitalizarPrimero(detalle.descripcion ?? ""),
          });
        } else if (!factor.competenciaId && factor.nombreCompetencia) {
          const coincidencia = listaCompetencias.find(
            (c) => c.nombre.toLowerCase() === factor.nombreCompetencia?.toLowerCase()
          );
          if (coincidencia) {
            setFormulario((prev) => ({ ...prev, competenciaId: String(coincidencia.id) }));
          }
        }
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        const mensaje =
          error instanceof Error ? error.message : "No se pudieron cargar las competencias.";
        toast.error("Error al cargar datos", mensaje);
      })
      .finally(() => {
        if (!controller.signal.aborted) setCargandoDatos(false);
      });

    return () => controller.abort();
  }, [abierto, factor.id, factor.competenciaId, factor.nombreCompetencia, toast]);

  function abrir() {
    setFormulario({
      competenciaId: factor.competenciaId ? String(factor.competenciaId) : "",
      codigo: capitalizarPrimero(factor.codigo ?? ""),
      nombre: capitalizarPrimero(factor.nombre ?? ""),
      orden: String(factor.orden ?? ""),
      descripcion: capitalizarPrimero(factor.descripcion ?? ""),
    });
    setErrores({});
    setCargandoDatos(true);
    setAbierto(true);
  }

  function cerrar() {
    setErrores({});
    setGuardando(false);
    setCargandoDatos(false);
    setAbierto(false);
  }

  function validar(): boolean {
    const nuevosErrores: Errores = {};

    if (!formulario.competenciaId.trim()) {
      nuevosErrores.competenciaId = "Debe seleccionar una competencia.";
    } else if (!/^[1-9]\d*$/.test(formulario.competenciaId.trim())) {
      nuevosErrores.competenciaId = "La competencia seleccionada no es válida.";
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

  async function handleSubmit(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!validar() || factor.id == null) return;

    setGuardando(true);
    try {
      await modificarFactor(factor.id, {
        competenciaId: Number(formulario.competenciaId.trim()),
        codigo: capitalizarPrimero(formulario.codigo.trim()),
        nombre: capitalizarPrimero(formulario.nombre.trim()),
        orden: Number(formulario.orden.trim()),
        descripcion: capitalizarPrimero(formulario.descripcion.trim()),
      });

      toast.success("Factor actualizado", "La operación ha culminado con éxito.");
      cerrar();
      onModificado?.();
    } catch (error) {
      const mensaje =
        error instanceof Error ? error.message : "No se pudo modificar el factor.";
      toast.error("Error al modificar", mensaje);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={(open) => {
        if (!guardando) {
          if (!open) cerrar();
          else abrir();
        }
      }}
      title="Modificar Factor"
      description={`Editá los datos del factor "${factor.nombre}".`}
      trigger={
        <Button type="button" variant="secondary">
          Modificar
        </Button>
      }
      size="md"
      closeOnEscape={!guardando}
      showCloseButton={!guardando}
      actions={
        <>
          <Button
            type="button"
            variant="secondary"
            disabled={guardando}
            onClick={cerrar}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            form="form-modificar-factor"
            loading={guardando}
            disabled={guardando}
          >
            {guardando ? "Guardando…" : "Aceptar"}
          </Button>
        </>
      }
    >
      <form id="form-modificar-factor" onSubmit={handleSubmit} noValidate>
        <Stack gap="md">
          <TextField
            label="Código (no editable)"
            name="codigo"
            value={formulario.codigo}
            onChange={() => {}}
            disabled
            helpText="El código del factor no puede modificarse."
          />

          <div className={styles.fieldGroup}>
            <label htmlFor="modificar-competencia" className={styles.selectLabel}>
              Competencia *
            </label>
            <select
              id="modificar-competencia"
              name="competenciaId"
              className={styles.selectControl}
              value={formulario.competenciaId}
              disabled={guardando || cargandoDatos}
              aria-invalid={Boolean(errores.competenciaId)}
              onChange={(e) => {
                const competenciaId = e.target.value;
                setFormulario((f) => ({ ...f, competenciaId }));
                if (errores.competenciaId) {
                  setErrores((err) => ({ ...err, competenciaId: undefined }));
                }
              }}
            >
              <option value="">
                {cargandoDatos ? "Cargando competencias…" : "Seleccionar competencia…"}
              </option>
              {competencias.map((comp) => (
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
            uppercase={true}
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
            uppercase={true}
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
    </Modal>
  );
}
