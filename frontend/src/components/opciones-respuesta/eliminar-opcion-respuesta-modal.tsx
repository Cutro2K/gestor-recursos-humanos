"use client";

import { useEffect, useId, useState } from "react";
import { Button, Modal, Spinner, Stack, Surface, useToast } from "react-ui-lib";
import { Feedback } from "../ui/feedback";
import { ApiError } from "@/service/api";
import {
  consultarUsoOpcionRespuesta,
  eliminarOpcionRespuesta,
} from "@/service/opciones-respuesta";
import type { ListadoOpcionRespuestaDTO } from "@/service/opciones-respuesta";
import styles from "./opciones-respuesta.module.css";

type EliminarOpcionRespuestaModalProps = {
  opcion: ListadoOpcionRespuestaDTO;
  onEliminada?: () => void;
};

/**
 * Etapas del CU 31: el sistema constata el uso antes de pedir confirmación (paso 2);
 * si la opción tiene preguntas asociadas muestra el aviso (2.A) en lugar de la confirmación.
 */
type Etapa = "verificando" | "confirmar" | "enUso" | "error";

function mensajeEnUso(nombre: string) {
  return `La opción de respuesta ${nombre} está siendo usada en la base de datos y no puede eliminarse.`;
}

export function EliminarOpcionRespuestaModal({
  opcion,
  onEliminada,
}: EliminarOpcionRespuestaModalProps) {
  const uid = useId();
  const idAceptar = `aceptar-uso-${uid}`;
  const [abierto, setAbierto] = useState(false);
  const [etapa, setEtapa] = useState<Etapa>("verificando");
  const [mensaje, setMensaje] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);
  const [eliminando, setEliminando] = useState(false);
  const toast = useToast();

  useEffect(() => {
    if (!abierto) return;
    const controller = new AbortController();

    void consultarUsoOpcionRespuesta(opcion.id, controller.signal)
      .then(({ enUso }) => {
        if (controller.signal.aborted) return;
        setMensaje(enUso ? mensajeEnUso(opcion.nombre) : null);
        setEtapa(enUso ? "enUso" : "confirmar");
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setMensaje(
          error instanceof Error
            ? error.message
            : "No pudimos verificar si la opción de respuesta está en uso.",
        );
        setEtapa("error");
      });

    return () => controller.abort();
  }, [abierto, intento, opcion.id, opcion.nombre]);

  function cambiarEstadoModal(open: boolean) {
    if (eliminando) return;
    if (open) {
      setEtapa("verificando");
      setMensaje(null);
    }
    setAbierto(open);
  }

  function reintentar() {
    setEtapa("verificando");
    setMensaje(null);
    setIntento((previo) => previo + 1);
  }

  async function confirmarEliminacion() {
    setEliminando(true);
    try {
      await eliminarOpcionRespuesta(opcion.id);
      toast.success(
        "Opción de respuesta eliminada",
        `Los datos de la opción de respuesta ${opcion.nombre} han sido eliminados del sistema.`,
      );
      setAbierto(false);
      onEliminada?.();
    } catch (error) {
      // Una pregunta pudo asociarse entre la verificación y la confirmación: el backend vuelve a constatarlo.
      if (error instanceof ApiError && error.status === 409) {
        setMensaje(error.message || mensajeEnUso(opcion.nombre));
        setEtapa("enUso");
      } else {
        const texto =
          error instanceof Error
            ? error.message
            : "No se pudo completar la eliminación de la opción de respuesta.";
        toast.error("Error al eliminar", texto);
      }
    } finally {
      setEliminando(false);
    }
  }

  const titulo =
    etapa === "enUso"
      ? "No se puede eliminar la opción de respuesta"
      : "Dar de baja Opción de Respuesta";

  const descripcion =
    etapa === "confirmar"
      ? `Los datos de la opción de respuesta ${opcion.nombre} serán eliminados del sistema.`
      : etapa === "verificando"
        ? "Verificando si la opción de respuesta está en uso…"
        : undefined;

  const acciones =
    etapa === "confirmar" ? (
      <>
        <Button
          type="button"
          variant="secondary"
          disabled={eliminando}
          onClick={() => cambiarEstadoModal(false)}
        >
          Cancelar
        </Button>
        <Button
          type="button"
          variant="destructive"
          loading={eliminando}
          disabled={eliminando}
          onClick={() => void confirmarEliminacion()}
        >
          {eliminando ? "Eliminando…" : "Eliminar"}
        </Button>
      </>
    ) : etapa === "error" ? (
      <>
        <Button type="button" variant="secondary" onClick={() => cambiarEstadoModal(false)}>
          Cancelar
        </Button>
        <Button type="button" variant="primary" onClick={reintentar}>
          Reintentar
        </Button>
      </>
    ) : etapa === "enUso" ? (
      <Button
        id={idAceptar}
        type="button"
        variant="primary"
        onClick={() => cambiarEstadoModal(false)}
      >
        Aceptar
      </Button>
    ) : (
      <Button type="button" variant="secondary" onClick={() => cambiarEstadoModal(false)}>
        Cancelar
      </Button>
    );

  return (
    <Modal
      open={abierto}
      onOpenChange={cambiarEstadoModal}
      title={titulo}
      description={descripcion}
      trigger={
        <Button type="button" variant="destructive">
          Eliminar
        </Button>
      }
      size="sm"
      closeOnEscape={!eliminando}
      showCloseButton={!eliminando}
      initialFocusId={etapa === "enUso" ? idAceptar : undefined}
      actions={acciones}
    >
      <Stack gap="sm">
        {etapa === "verificando" && (
          <div className={styles.status} role="status">
            <Spinner size="sm" aria-hidden="true" />
            <span>Verificando uso…</span>
          </div>
        )}
        {etapa === "enUso" && mensaje && (
          <Feedback title="La opción de respuesta está en uso" variant="error">
            {mensaje}
          </Feedback>
        )}
        {etapa === "error" && mensaje && (
          <Feedback title="No pudimos verificar el uso" variant="error">
            {mensaje}
          </Feedback>
        )}
        <Surface bordered padding="sm">
          <Stack gap="sm">
            <span className="font-semibold">{opcion.nombre}</span>
            <span className={styles.muted}>{opcion.descripcion || "—"}</span>
          </Stack>
        </Surface>
      </Stack>
    </Modal>
  );
}
