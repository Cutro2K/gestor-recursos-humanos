"use client";

import { Button, Stack } from "react-ui-lib";
import { Feedback } from "@/components/ui/feedback";

export default function PageError({ retry }: { error: unknown; retry: () => void }) {
  return (
    <Stack gap="lg">
      <Feedback variant="error" title="No pudimos cargar esta sección">
        Volvé a intentarlo. Si el problema continúa, regresá al inicio.
      </Feedback>
      <Button onClick={() => retry()} className="self-start">
        Reintentar
      </Button>
    </Stack>
  );
}
