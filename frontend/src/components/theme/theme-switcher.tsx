"use client";

import { Button } from "react-ui-lib";
import { Icon } from "@/components/ui/icon";
import { useTheme } from "./theme-provider";

export function ThemeSwitcher() {
  const { resolvedTheme, setTheme } = useTheme();
  const dark = resolvedTheme === "dark";
  const label = dark ? "Cambiar a tema claro" : "Cambiar a tema oscuro";

  return (
    <Button
      variant="secondary"
      size="sm"
      className="size-9 min-h-0 rounded-full p-0"
      aria-label={label}
      title={label}
      onClick={() => setTheme(dark ? "light" : "dark")}
    >
      <Icon name={dark ? "sun" : "moon"} />
    </Button>
  );
}
