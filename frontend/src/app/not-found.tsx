import { Stack } from "react-ui-lib";
import { PageHeader } from "@/components/ui/page-header";
import { TextLink } from "@/components/ui/text-link";

export default function NotFound() {
  return (
    <Stack gap="lg" align="start">
      <PageHeader
        eyebrow="Página no encontrada"
        title="Este camino no lleva a una sección."
        description="Revisá la dirección o elegí una sección de la navegación."
      />
      <TextLink href="/">Volver al inicio</TextLink>
    </Stack>
  );
}
