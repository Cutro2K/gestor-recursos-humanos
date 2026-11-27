"use client";

import { useState } from "react";
import { Button, Modal, Stack, Surface, useToast } from "react-ui-lib";
import { Feedback } from "../ui/feedback";
import { eliminarOpcionRespuesta } from "@/service/opciones-respuesta";
import type { ListadoOpcionRespuestaDTO } from "@/service/opciones-respuesta";
import styles from "./opciones-respuesta.module.css";

type EliminarOpcionRespuestaModalProps = {
  opcion: ListadoOpcionRespuestaDTO;
  onEliminada?: () => void;
};

export function EliminarOpcionRespuestaModal({
  opcion,
  onEliminada,
}: EliminarOpcionRespuestaModalProps) {
  const [abierto, setAbierto] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const [errorEliminacion, setErrorEliminacion] = useState<string | null>(null);
  const toast = useToast();

  function cambiarEstadoModal(open: boolean) {
    if (eliminando) return;
    setErrorEliminacion(null);
    setAbierto(open);
  }

  async function confirmarEliminacion() {
    setEliminando(true);
    setErrorEliminacion(null);
    try {
      await eliminarOpcionRespuesta(opcion.id);
      toast.success(
        "Opción de respuesta eliminada",
        `Los datos de la opción de respuesta ${opcion.nombre} han sido eliminados del sistema.`,
      );
      setAbierto(false);
      onEliminada?.();
    } catch (error) {
      const mensaje =
        error instanceof Error
          ? error.message
          : "No se pudo completar la eliminación de la opción de respuesta.";
      setErrorEliminacion(mensaje);
      toast.error("Error al eliminar", mensaje);
    } finally {
      setEliminando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={cambiarEstadoModal}
      title="Dar de baja Opción de Respuesta"
      description={`Los datos de la opción de respuesta ${opcion.nombre} serán eliminados del sistema.`}
      trigger={
        <Button type="button" variant="destructive">
          Eliminar
        </Button>
      }
      size="sm"
      closeOnEscape={!eliminando}
      showCloseButton={!eliminando}
      actions={
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
      }
    >
      <Stack gap="sm">
        {errorEliminacion && (
          <Feedback
            title="No se pudo eliminar la opción de respuesta"
            variant="error"
          >
            {errorEliminacion}
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
