import Link from "next/link";
import { Stack, Surface } from "react-ui-lib";
import type { Section } from "@/config/sections";
import { Icon } from "./icon";

export function SectionLink({ section }: { section: Section }) {
  return (
    <Link href={`/${section.id}`} className="block h-full">
      <Surface bordered className="h-full">
        <Stack direction="row" align="center" gap="md">
          <Icon name={section.icon} className="shrink-0 text-[var(--ui-accent)]" />
          <Stack gap="sm" className="min-w-0 flex-1">
            <strong className="text-sm font-semibold">{section.title}</strong>
            <p className="text-sm text-[var(--ui-muted)]">{section.description}</p>
          </Stack>
          <Icon name="arrow" className="shrink-0" />
        </Stack>
      </Surface>
    </Link>
  );
}
