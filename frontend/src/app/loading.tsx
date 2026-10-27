import { Spinner, Stack } from "react-ui-lib";

export default function Loading() {
  return (
    <div role="status">
      <Stack
        direction="row"
        align="center"
        justify="center"
        gap="sm"
        className="min-h-64 text-[var(--ui-muted)]"
      >
        <Spinner aria-label="Cargando sección" />
        <p>Cargando sección…</p>
      </Stack>
    </div>
  );
}
