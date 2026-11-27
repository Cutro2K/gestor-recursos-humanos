"use client";

import { useEffect, useId, useState } from "react";
import type { FormEvent } from "react";
import { Button, Modal, Spinner, Stack, TextField, useToast } from "react-ui-lib";
import { Feedback } from "../ui/feedback";
import { ApiError } from "@/service/api";
import {
  modificarOpcionRespuesta,
  obtenerOpcionRespuestaPorId,
} from "@/service/opciones-respuesta";
import type { ListadoOpcionRespuestaDTO } from "@/service/opciones-respuesta";
import {
  LIMITES_OPCION_RESPUESTA,
  crearErroresVacios,
  mapearErroresBackendModificacion,
  sinErroresModificacionOpcionRespuesta,
  validarModificacionOpcionRespuesta,
} from "./opciones-respuesta-validation";
import type { ErroresModificacionOpcionRespuesta } from "./opciones-respuesta-validation";
import styles from "./opciones-respuesta.module.css";

type FilaItemExistenteState = {
  id: number;
  orden: string;
  descripcion: string;
};

type ModificarOpcionRespuestaModalProps = {
  opcion: ListadoOpcionRespuestaDTO;
  onModificada?: () => void;
};

export function ModificarOpcionRespuestaModal({
  opcion,
  onModificada,
}: ModificarOpcionRespuestaModalProps) {
  const uid = useId();
  const formId = `form-modificar-opcion-${uid}`;
  const toast = useToast();

  const [abierto, setAbierto] = useState(false);
  const [descripcion, setDescripcion] = useState((opcion.descripcion ?? "").toUpperCase());
  const [filas, setFilas] = useState<FilaItemExistenteState[]>([]);
  const [cargandoDetalle, setCargandoDetalle] = useState(false);
  const [errorCargaDetalle, setErrorCargaDetalle] = useState<string | null>(null);
  const [intentoCarga, setIntentoCarga] = useState(0);
  const [errores, setErrores] = useState<ErroresModificacionOpcionRespuesta>(() =>
    crearErroresVacios(0),
  );
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    if (!abierto) return;
    const controller = new AbortController();

    void obtenerOpcionRespuestaPorId(opcion.id, controller.signal)
      .then((detalle) => {
        if (controller.signal.aborted) return;
        setDescripcion((detalle.descripcion ?? opcion.descripcion ?? "").toUpperCase());
        const filasCargadas: FilaItemExistenteState[] = (detalle.items ?? []).map(
          (item, indice) => ({
            id: Number(item.id ?? indice + 1),
            orden: String(item.orden ?? indice + 1),
            descripcion: (item.descripcion ?? "").toUpperCase(),
          }),
        );
        setFilas(filasCargadas);
        setErrores(crearErroresVacios(filasCargadas.length));
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        const mensaje =
          err instanceof Error
            ? err.message
            : "No se pudieron cargar los ítems de opción de la opción de respuesta.";
        setErrorCargaDetalle(mensaje);
        toast.error("Error al cargar datos", mensaje);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setCargandoDetalle(false);
        }
      });

    return () => controller.abort();
  }, [abierto, opcion.id, opcion.descripcion, intentoCarga, toast]);

  function abrir() {
    setDescripcion((opcion.descripcion ?? "").toUpperCase());
    setFilas([]);
    setErrores(crearErroresVacios(0));
    setErrorServidor(null);
    setErrorCargaDetalle(null);
    setCargandoDetalle(true);
    setAbierto(true);
  }

  function cerrar() {
    setErrores(crearErroresVacios(0));
    setErrorServidor(null);
    setErrorCargaDetalle(null);
    setCargandoDetalle(false);
    setGuardando(false);
    setAbierto(false);
  }

  function reintentarCarga() {
    setErrorCargaDetalle(null);
    setCargandoDetalle(true);
    setIntentoCarga((prev) => prev + 1);
  }

  function actualizarFila(indice: number, campo: "orden" | "descripcion", valor: string) {
    setFilas((actuales) =>
      actuales.map((fila, idx) => (idx === indice ? { ...fila, [campo]: valor } : fila)),
    );
    setErrores((prev) => {
      const nuevasFilas = prev.filas.map((errFila, idx) =>
        idx === indice ? { ...errFila, [campo]: undefined, id: undefined } : errFila,
      );
      return {
        ...prev,
        items: campo === "orden" ? undefined : prev.items,
        filas: nuevasFilas,
      };
    });
    setErrorServidor(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (cargandoDetalle || errorCargaDetalle) return;

    const validacion = validarModificacionOpcionRespuesta({
      descripcion,
      items: filas.map((f) => ({ orden: f.orden, descripcion: f.descripcion })),
    });
    setErrores(validacion);
    setErrorServidor(null);

    if (!sinErroresModificacionOpcionRespuesta(validacion)) {
      return;
    }

    setGuardando(true);
    try {
      await modificarOpcionRespuesta(opcion.id, {
        descripcion: descripcion.trim().toUpperCase(),
        items: filas.map((fila) => ({
          id: fila.id,
          orden: Number(fila.orden.trim()),
          descripcion: fila.descripcion.trim().toUpperCase(),
        })),
      });

      toast.success("Opción de respuesta actualizada", "La operación ha culminado con éxito.");
      cerrar();
      onModificada?.();
    } catch (err) {
      const mensaje =
        err instanceof Error
          ? err.message
          : "No se pudo modificar la opción de respuesta.";

      if (err instanceof ApiError) {
        const detalle = err.details as { erroresValidacion?: Record<string, string> } | undefined;
        const mapeados = mapearErroresBackendModificacion(
          detalle?.erroresValidacion,
          filas.length,
        );
        if (mapeados) {
          setErrores(mapeados);
        }
      }

      setErrorServidor(mensaje);
      toast.error("Error al modificar", mensaje);
    } finally {
      setGuardando(false);
    }
  }

  const bloquearAceptar =
    guardando || cargandoDetalle || Boolean(errorCargaDetalle) || filas.length === 0;

  return (
    <Modal
      open={abierto}
      onOpenChange={(open) => {
        if (guardando) return;
        if (!open) cerrar();
        else abrir();
      }}
      title="Modificar Opción de Respuesta"
      description={`Editá la descripción y los ítems de opción de "${opcion.nombre}" (CU-030).`}
      trigger={
        <Button type="button" variant="secondary">
          Modificar
        </Button>
      }
      size="lg"
      style={{ maxHeight: "calc(100dvh - 2rem)" }}
      closeOnEscape={!guardando}
      closeOnOutsideClick={false}
      showCloseButton={!guardando}
      actions={
        <>
          <Button
            key="cancelar"
            type="button"
            variant="secondary"
            disabled={guardando}
            onClick={cerrar}
          >
            Cancelar
          </Button>
          <Button
            key="aceptar"
            type="submit"
            form={formId}
            loading={guardando}
            disabled={bloquearAceptar}
          >
            {guardando ? "Guardando…" : "Aceptar"}
          </Button>
        </>
      }
    >
      <form id={formId} onSubmit={(e) => void handleSubmit(e)} noValidate>
        <Stack gap="md">
          {errorServidor && (
            <Feedback
              title="No se pudo modificar la opción de respuesta"
              variant="error"
            >
              {errorServidor}
            </Feedback>
          )}

          <TextField
            label="Nombre (no editable)"
            name="nombre"
            value={opcion.nombre}
            onChange={() => {}}
            disabled
            helpText="El nombre de la opción de respuesta no puede modificarse."
          />

          <TextField
            uppercase={true}
            label="Descripción *"
            name="descripcion"
            value={descripcion}
            onChange={(valor) => {
              setDescripcion(valor.toUpperCase());
              if (errores.descripcion) {
                setErrores((prev) => ({ ...prev, descripcion: undefined }));
              }
              setErrorServidor(null);
            }}
            maxLength={LIMITES_OPCION_RESPUESTA.descripcion}
            error={errores.descripcion}
            placeholder="Ej: ESCALA VALORATIVA DE CUMPLIMIENTO"
            disabled={guardando || cargandoDetalle}
            autoComplete="off"
            required
          />

          <div className={styles.itemsSection}>
            <div className={styles.itemsHeader}>
              <div>
                <h3 className="text-sm font-semibold">Ítems de opción *</h3>
                <p className={styles.muted}>
                  Modificá el orden de visualización consecutivo a partir de 1 y la descripción de los ítems existentes.
                </p>
              </div>
            </div>

            {cargandoDetalle ? (
              <div className={styles.status} role="status">
                <Spinner size="sm" aria-hidden="true" />
                <span>Cargando ítems de opción…</span>
              </div>
            ) : errorCargaDetalle ? (
              <Stack gap="sm">
                <Feedback
                  title="No se pudieron cargar los ítems de opción"
                  variant="error"
                >
                  {errorCargaDetalle}
                </Feedback>
                <div>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={reintentarCarga}
                  >
                    Reintentar
                  </Button>
                </div>
              </Stack>
            ) : (
              <>
                {errores.items && (
                  <p className={styles.fieldError} role="alert">
                    {errores.items}
                  </p>
                )}

                <div className={styles.itemsList}>
                  {filas.map((fila, indice) => (
                    <div key={fila.id} className={styles.itemRowFixed}>
                      <div className={styles.itemOrderCol}>
                        <TextField
                          label="Orden *"
                          name={`orden-${fila.id}`}
                          type="number"
                          value={fila.orden}
                          onChange={(valor) => actualizarFila(indice, "orden", valor)}
                          error={errores.filas[indice]?.orden}
                          placeholder={String(indice + 1)}
                          disabled={guardando}
                          required
                        />
                      </div>

                      <div className={styles.itemDescCol}>
                        <TextField
                          uppercase={true}
                          label="Descripción *"
                          name={`descripcion-item-${fila.id}`}
                          value={fila.descripcion}
                          onChange={(valor) =>
                            actualizarFila(indice, "descripcion", valor.toUpperCase())
                          }
                          maxLength={LIMITES_OPCION_RESPUESTA.descripcionItem}
                          error={errores.filas[indice]?.descripcion}
                          placeholder={`Ej: OPCIÓN ${indice + 1}`}
                          disabled={guardando}
                          autoComplete="off"
                          required
                        />
                        {errores.filas[indice]?.id && (
                          <p className={styles.fieldError} role="alert">
                            {errores.filas[indice].id}
                          </p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        </Stack>
      </form>
    </Modal>
  );
}
