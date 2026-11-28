"use client";

import { useState } from "react";
import { Button, Modal, Stack, Surface } from "react-ui-lib";

export type AccionEnDesarrolloModalProps = {
  etiquetaBoton: string;
  variant?: "primary" | "secondary" | "destructive";
  size?: "sm" | "md" | "lg";
  titulo: string;
  nombreRecurso?: string;
  detalle?: string;
};

export function AccionEnDesarrolloModal({
  etiquetaBoton,
  variant = "secondary",
  size = "sm",
  titulo,
  nombreRecurso,
  detalle,
}: AccionEnDesarrolloModalProps) {
  const [abierto, setAbierto] = useState(false);

  return (
    <Modal
      open={abierto}
      onOpenChange={setAbierto}
      title={titulo}
      description="Funcionalidad en desarrollo."
      trigger={
        <Button type="button" variant={variant} size={size}>
          {etiquetaBoton}
        </Button>
      }
      size="sm"
      closeOnEscape
      showCloseButton
      actions={
        <Button type="button" variant="primary" onClick={() => setAbierto(false)}>
          Entendido
        </Button>
      }
    >
      <Surface bordered padding="md">
        <Stack gap="sm">
          <p className="text-sm font-medium">
            {nombreRecurso ? (
              <>
                La operación solicitada sobre <strong>{nombreRecurso}</strong> se encuentra
                actualmente en desarrollo.
              </>
            ) : (
              "Esta funcionalidad se encuentra actualmente en desarrollo."
            )}
          </p>
          {detalle && (
            <p className="text-sm leading-relaxed text-[var(--ui-muted)]">{detalle}</p>
          )}
        </Stack>
      </Surface>
    </Modal>
  );
}
