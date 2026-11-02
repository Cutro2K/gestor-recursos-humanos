import { Stack } from "react-ui-lib";
import { AppShell } from "@/components/layout/app-shell";
import { PageHeader } from "@/components/ui/page-header";
import { TextLink } from "@/components/ui/text-link";

export default function NotFound() {
  // Queda fuera de (sistema) para atrapar cualquier URL; usa el shell igual.
  return (
    <AppShell>
      <Stack gap="lg" align="start">
        <PageHeader
          eyebrow="Página no encontrada"
          title="Este camino no lleva a una sección."
          description="Revisá la dirección o elegí una sección de la navegación."
        />
        <TextLink href="/">Volver al inicio</TextLink>
      </Stack>
    </AppShell>
  );
}
