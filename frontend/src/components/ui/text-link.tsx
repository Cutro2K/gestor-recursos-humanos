import Link from "next/link";
import type { ComponentProps } from "react";

/** Text navigation; ReactUILib has no link primitive. */
export function TextLink({ className = "", ...props }: ComponentProps<typeof Link>) {
  return (
    <Link
      className={`inline-flex items-center gap-2 text-[var(--ui-accent)] underline underline-offset-4 ${className}`}
      {...props}
    />
  );
}
