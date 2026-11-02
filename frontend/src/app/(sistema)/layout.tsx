import { AppShell } from "@/components/layout/app-shell";

/** Rutas del sistema: requieren sesión (ver src/proxy.ts) y comparten el shell. */
export default function SistemaLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}
