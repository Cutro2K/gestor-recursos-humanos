"use client";

import { Button, Stack } from "react-ui-lib";
import { Icon } from "./icon";

/** Estado persistente dentro de la sección afectada, sin overlay ni bloqueo de navegación. */
export function RequestError({
  title,
  description,
  onRetry,
  retryLabel = "Reintentar conexión",
  retrying = false,
}: {
  title: string;
  description: string;
  onRetry: () => void;
  retryLabel?: string;
  retrying?: boolean;
}) {
  return (
    <section
      aria-label={title}
      aria-busy={retrying}
      className="flex flex-1 items-center justify-center py-8"
    >
      <Stack
        gap="md"
        align="center"
        className="w-full max-w-lg rounded-2xl border border-[var(--ui-border)] bg-[var(--ui-surface)] p-6 text-center"
      >
        <Icon name="info" className="shrink-0 text-[var(--ui-danger)]" />
        <div role="status">
          <h2 className="font-semibold">{title}</h2>
          <p className="mt-2 text-sm text-[var(--ui-muted)]">{description}</p>
        </div>
        <Button variant="secondary" loading={retrying} onClick={onRetry}>
          {retrying ? "Reintentando…" : retryLabel}
        </Button>
      </Stack>
    </section>
  );
}
