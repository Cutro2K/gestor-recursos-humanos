"use client";

import { useState } from "react";
import { Button, Modal, Stack, Surface, useToast } from "react-ui-lib";
import { eliminarFactor, eliminarFactores } from "@/service/factores";
import type { Factor } from "@/service/factores";
import styles from "./factores.module.css";

type EliminarFactorModalProps = {
  factores: Factor[];
  onEliminado?: () => void;
};

export function EliminarFactorModal({ factores, onEliminado }: EliminarFactorModalProps) {
  const [abierto, setAbierto] = useState(false);
  const [eliminando, setEliminando] = useState(false);
  const toast = useToast();

  const cantidad = factores.length;
  const esMultiple = cantidad > 1;
  const unicoFactor = cantidad === 1 ? factores[0] : null;

  async function confirmarEliminacion() {
    const ids = factores
      .map((f) => f.id)
      .filter((id): id is number => typeof id === "number");

    if (!ids.length) return;

    setEliminando(true);
    try {
      if (ids.length === 1) {
        const nombreFactor = unicoFactor?.nombre ?? "";
        await eliminarFactor(ids[0]);
        toast.success(
          "Factor eliminado",
          `Los datos del factor ${nombreFactor} han sido eliminados del sistema.`
        );
      } else {
        await eliminarFactores(ids);
        toast.success(
          "Factores eliminados",
          `Los datos de los ${ids.length} factores seleccionados han sido eliminados del sistema.`
        );
      }

      setAbierto(false);
      onEliminado?.();
    } catch (error) {
      const mensaje =
        error instanceof Error ? error.message : "No se pudo completar la eliminación.";
      toast.error("Error al eliminar", mensaje);
    } finally {
      setEliminando(false);
    }
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={(open) => {
        if (!eliminando) setAbierto(open);
      }}
      title={esMultiple ? "Dar de baja Factores" : "Dar de baja Factor"}
      description={
        esMultiple
          ? `Los datos de los ${cantidad} factores seleccionados y sus preguntas serán eliminados del sistema.`
          : `Los datos del factor ${unicoFactor?.nombre ?? ""} y sus preguntas serán eliminados del sistema.`
      }
      trigger={
        <Button type="button" variant="destructive">
          {esMultiple ? `Eliminar seleccionados (${cantidad})` : "Eliminar"}
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
            onClick={() => setAbierto(false)}
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
        {unicoFactor ? (
          <Surface bordered padding="sm">
            <Stack gap="xs">
              <span className="font-semibold">{unicoFactor.nombre}</span>
              <span className={styles.muted}>
                Código: <strong>{unicoFactor.codigo}</strong> · Competencia:{" "}
                {unicoFactor.nombreCompetencia ?? `#${unicoFactor.competenciaId}`}
              </span>
            </Stack>
          </Surface>
        ) : (
          <Surface bordered padding="sm">
            <p className="mb-2 text-sm font-semibold">Factores a dar de baja ({cantidad}):</p>
            <ul className="max-h-40 list-disc space-y-1 overflow-y-auto pl-5 text-sm">
              {factores.map((factor) => (
                <li key={factor.id ?? factor.codigo}>
                  <span className="font-medium">{factor.nombre}</span>{" "}
                  <span className={styles.muted}>({factor.codigo})</span>
                </li>
              ))}
            </ul>
          </Surface>
        )}
      </Stack>
    </Modal>
  );
}
