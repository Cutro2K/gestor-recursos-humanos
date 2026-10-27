import type { ReactNode } from "react";
import { Stack } from "react-ui-lib";

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <header>
      <Stack direction="row" mobileDirection="column" justify="between" gap="lg">
        <Stack gap="sm">
          {eyebrow && (
            <p className="text-xs font-semibold uppercase tracking-wide text-[var(--ui-muted)]">
              {eyebrow}
            </p>
          )}
          <h1 className="text-3xl font-semibold tracking-tight">{title}</h1>
          <p className="max-w-2xl text-sm leading-relaxed text-[var(--ui-muted)]">{description}</p>
        </Stack>
        {actions && <div className="shrink-0">{actions}</div>}
      </Stack>
    </header>
  );
}
