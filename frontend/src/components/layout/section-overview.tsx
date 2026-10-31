import { Stack, Surface } from "react-ui-lib";
import { getSection } from "@/config/sections";
import type { SectionId } from "@/config/sections";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { SectionLink } from "@/components/ui/section-link";

export function SectionOverview({ id }: { id: SectionId }) {
  const section = getSection(id)!;

  return (
    <Stack gap="lg">
      <PageHeader
        eyebrow={section.group}
        title={section.title}
        description={section.description}
        actions={<span className="text-sm text-[var(--ui-muted)]">Próximamente</span>}
      />
      <Surface bordered padding="lg">
        <Stack gap="lg" align="center" justify="center" className="min-h-64 text-center">
          <Icon name={section.icon} width="32" height="32" className="text-[var(--ui-accent)]" />
          <h2 className="text-xl font-semibold">
            Un espacio para {section.title.toLocaleLowerCase("es")}
          </h2>
          <p className="max-w-lg text-sm leading-relaxed text-[var(--ui-muted)]">
            Próximamente vas a poder{" "}
            {section.upcoming.charAt(0).toLocaleLowerCase("es") + section.upcoming.slice(1)}
          </p>
        </Stack>
      </Surface>
      <section aria-labelledby="related-title">
        <Stack gap="md">
          <h2 id="related-title" className="text-lg font-semibold">
            También en tu espacio de trabajo
          </h2>
          <div className="grid gap-4 md:grid-cols-2">
            {section.related.map((id) => (
              <SectionLink key={id} section={getSection(id)!} />
            ))}
          </div>
        </Stack>
      </section>
    </Stack>
  );
}
