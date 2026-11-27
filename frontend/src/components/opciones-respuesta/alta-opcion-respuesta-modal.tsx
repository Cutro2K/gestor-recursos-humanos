"use client";

import { useId, useRef, useState } from "react";
import type { FormEvent, MouseEvent } from "react";
import { Button, Modal, Stack, Surface, TextField } from "react-ui-lib";
import { Feedback } from "../ui/feedback";
import { ApiError } from "@/service/api";
import { crearOpcionRespuesta } from "@/service/opciones-respuesta";
import {
  LIMITES_OPCION_RESPUESTA,
  crearErroresVacios,
  mapearErroresBackendAlta,
  sinErroresAltaOpcionRespuesta,
  sugerirSiguienteOrden,
  validarAltaOpcionRespuesta,
} from "./opciones-respuesta-validation";
import type { ErroresAltaOpcionRespuesta } from "./opciones-respuesta-validation";
import styles from "./opciones-respuesta.module.css";

type FilaItemState = {
  key: number;
  orden: string;
  descripcion: string;
};

function crearFilasIniciales(nextKey: () => number): FilaItemState[] {
  return [
    { key: nextKey(), orden: "1", descripcion: "" },
    { key: nextKey(), orden: "2", descripcion: "" },
  ];
}

export function AltaOpcionRespuestaModal({ onCreada }: { onCreada?: () => void }) {
  const uid = useId();
  const formId = `form-alta-opcion-${uid}`;
  const seqRef = useRef(1);
  const obtenerKey = () => seqRef.current++;

  const [abierto, setAbierto] = useState(false);
  const [nombre, setNombre] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [filas, setFilas] = useState<FilaItemState[]>(() => [
    { key: 1, orden: "1", descripcion: "" },
    { key: 2, orden: "2", descripcion: "" },
  ]);
  const [errores, setErrores] = useState<ErroresAltaOpcionRespuesta>(() => crearErroresVacios(2));
  const [errorServidor, setErrorServidor] = useState<string | null>(null);
  const [guardando, setGuardando] = useState(false);
  const [opcionRecienCreada, setOpcionRecienCreada] = useState<string | null>(null);

  function reiniciarFormulario() {
    seqRef.current = Math.max(seqRef.current, 3);
    setNombre("");
    setDescripcion("");
    setFilas(crearFilasIniciales(obtenerKey));
    setErrores(crearErroresVacios(2));
    setErrorServidor(null);
    setGuardando(false);
    setOpcionRecienCreada(null);
  }

  function limpiarYCerrar() {
    reiniciarFormulario();
    setAbierto(false);
  }

  function cargarOtra(event?: MouseEvent<HTMLButtonElement>) {
    event?.preventDefault();
    reiniciarFormulario();
  }

  function agregarFila() {
    seqRef.current = Math.max(seqRef.current, 3);
    const nuevaKey = obtenerKey();
    setFilas((actuales) => [
      ...actuales,
      {
        key: nuevaKey,
        orden: sugerirSiguienteOrden(actuales.map((f) => f.orden)),
        descripcion: "",
      },
    ]);
    setErrores((prev) => ({
      ...prev,
      items: undefined,
      filas: [...prev.filas, {}],
    }));
    setErrorServidor(null);
  }

  function quitarFila(key: number) {
    if (filas.length <= LIMITES_OPCION_RESPUESTA.minItems) return;
    const indice = filas.findIndex((f) => f.key === key);
    if (indice === -1) return;

    setFilas((actuales) => actuales.filter((f) => f.key !== key));
    setErrores((prev) => ({
      ...prev,
      items: undefined,
      filas: prev.filas.filter((_, idx) => idx !== indice),
    }));
    setErrorServidor(null);
  }

  function actualizarFila(indice: number, campo: "orden" | "descripcion", valor: string) {
    setFilas((actuales) =>
      actuales.map((fila, idx) => (idx === indice ? { ...fila, [campo]: valor } : fila)),
    );
    setErrores((prev) => {
      const nuevasFilas = prev.filas.map((errFila, idx) =>
        idx === indice ? { ...errFila, [campo]: undefined } : errFila,
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

    const validacion = validarAltaOpcionRespuesta({
      nombre,
      descripcion,
      items: filas.map((f) => ({ orden: f.orden, descripcion: f.descripcion })),
    });
    setErrores(validacion);
    setErrorServidor(null);

    if (!sinErroresAltaOpcionRespuesta(validacion)) {
      return;
    }

    setGuardando(true);
    try {
      const nombreNormalizado = nombre.trim().toUpperCase();
      const creada = await crearOpcionRespuesta({
        nombre: nombreNormalizado,
        descripcion: descripcion.trim().toUpperCase(),
        items: filas.map((fila) => ({
          orden: Number(fila.orden.trim()),
          descripcion: fila.descripcion.trim().toUpperCase(),
        })),
      });

      onCreada?.();
      setOpcionRecienCreada(creada.nombre || nombreNormalizado);
    } catch (err) {
      if (err instanceof ApiError) {
        const detalle = err.details as { erroresValidacion?: Record<string, string> } | undefined;
        const mapeados = mapearErroresBackendAlta(detalle?.erroresValidacion, filas.length);
        if (mapeados) {
          setErrores(mapeados);
        } else {
          setErrorServidor(err.message);
        }
      } else {
        setErrorServidor(
          err instanceof Error
            ? err.message
            : "No se pudo registrar la opción de respuesta.",
        );
      }
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={(open) => {
        if (guardando) return;
        if (!open) limpiarYCerrar();
        else setAbierto(true);
      }}
      title={
        opcionRecienCreada
          ? "Opción de respuesta registrada"
          : "Dar de alta Opción de Respuesta"
      }
      description={
        opcionRecienCreada
          ? "Confirmación de operación (CU-029)."
          : "Ingresá el nombre, la descripción y al menos dos ítems de opción con orden consecutivo."
      }
      trigger={
        <Button type="button" variant="primary">
          Nueva opción de respuesta
        </Button>
      }
      size="lg"
      style={{ maxHeight: "calc(100dvh - 2rem)" }}
      closeOnEscape={!guardando}
      closeOnOutsideClick={false}
      showCloseButton={!guardando}
      actions={
        opcionRecienCreada ? (
          <>
            <Button key="no" type="button" variant="secondary" onClick={limpiarYCerrar}>
              No
            </Button>
            <Button key="si" type="button" variant="primary" onClick={cargarOtra}>
              Sí
            </Button>
          </>
        ) : (
          <>
            <Button
              key="cancelar"
              type="button"
              variant="secondary"
              disabled={guardando}
              onClick={limpiarYCerrar}
            >
              Cancelar
            </Button>
            <Button
              key="aceptar"
              type="submit"
              form={formId}
              loading={guardando}
              disabled={guardando}
            >
              {guardando ? "Guardando…" : "Aceptar"}
            </Button>
          </>
        )
      }
    >
      {opcionRecienCreada ? (
        <Surface bordered padding="md">
          <p className="text-base font-medium">
            La opción <strong>{opcionRecienCreada}</strong> se ha creado correctamente.{" "}
            ¿Desea cargar otra?
          </p>
        </Surface>
      ) : (
        <form id={formId} onSubmit={(e) => void handleSubmit(e)} noValidate>
          <Stack gap="md">
            {errorServidor && (
              <Feedback
                title="No se pudo registrar la opción de respuesta"
                variant="error"
              >
                {errorServidor}
              </Feedback>
            )}

            <TextField
              uppercase={true}
              label="Nombre *"
              name="nombre"
              value={nombre}
              onChange={(valor) => {
                setNombre(valor.toUpperCase());
                if (errores.nombre) {
                  setErrores((prev) => ({ ...prev, nombre: undefined }));
                }
                setErrorServidor(null);
              }}
              maxLength={LIMITES_OPCION_RESPUESTA.nombre}
              error={errores.nombre}
              placeholder="Ej: ESCALA DE DESEMPEÑO"
              disabled={guardando}
              autoComplete="off"
              required
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
              disabled={guardando}
              autoComplete="off"
              required
            />

            <div className={styles.itemsSection}>
              <div className={styles.itemsHeader}>
                <div>
                  <h3 className="text-sm font-semibold">Ítems de opción *</h3>
                  <p className={styles.muted}>
                    Definí al menos dos ítems con orden de visualización consecutivo a partir de 1.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  disabled={guardando}
                  onClick={agregarFila}
                >
                  Agregar opción
                </Button>
              </div>

              {errores.items && (
                <p className={styles.fieldError} role="alert">
                  {errores.items}
                </p>
              )}

              <div className={styles.itemsList}>
                {filas.map((fila, indice) => (
                  <div key={fila.key} className={styles.itemRow}>
                    <div className={styles.itemOrderCol}>
                      <TextField
                        label="Orden *"
                        name={`orden-${fila.key}`}
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
                        name={`descripcion-item-${fila.key}`}
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
                    </div>

                    <div className={styles.itemRemoveCol}>
                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        disabled={guardando || filas.length <= LIMITES_OPCION_RESPUESTA.minItems}
                        onClick={() => quitarFila(fila.key)}
                        aria-label={`Quitar ítem ${indice + 1}`}
                      >
                        Quitar
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Stack>
        </form>
      )}
    </Modal>
  );
}
