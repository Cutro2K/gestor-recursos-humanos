"use client";

import { useEffect, useTransition } from "react";
import { useToast } from "react-ui-lib";
import { RequestError } from "@/components/ui/request-error";

export default function PageError({ error, retry }: { error: unknown; retry: () => void }) {
  const [retrying, startTransition] = useTransition();
  const { error: showToast } = useToast();
  const title = "No pudimos cargar esta sección";
  const description = "Volvé a intentarlo. Si el problema continúa, regresá al inicio.";

  useEffect(() => {
    showToast(title, description);
  }, [error, showToast]);

  return (
    <RequestError
      title={title}
      description={description}
      retryLabel="Reintentar"
      retrying={retrying}
      onRetry={() => startTransition(retry)}
    />
  );
}
