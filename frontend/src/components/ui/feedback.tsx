import type { ReactNode } from "react";
import { Stack, Surface } from "react-ui-lib";
import { Icon } from "./icon";

/** Inline feedback composed from library primitives; transient feedback uses useToast. */
export function Feedback({
  title,
  children,
  variant = "info",
}: {
  title: string;
  children?: ReactNode;
  variant?: "info" | "success" | "error";
}) {
  return (
    <div role={variant === "error" ? "alert" : "status"}>
      <Surface bordered padding="sm">
        <Stack direction="row" align="start" gap="sm">
          <Icon
            name={variant === "success" ? "check" : "info"}
            className={`shrink-0 ${variant === "error" ? "text-[var(--ui-danger)]" : "text-[var(--ui-accent)]"}`}
          />
          <Stack gap="sm">
            <strong className="text-sm font-semibold">{title}</strong>
            {children && <p className="text-sm text-[var(--ui-muted)]">{children}</p>}
          </Stack>
        </Stack>
      </Surface>
    </div>
  );
}
