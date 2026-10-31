"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import type { ReactNode } from "react";
import { Button, Sidebar, Stack } from "react-ui-lib";
import { getSection, sections } from "@/config/sections";
import { Icon } from "@/components/ui/icon";
import { ThemeSwitcher } from "@/components/theme/theme-switcher";
import { UserProfile } from "./user-profile";
import styles from "./app-shell.module.css";

const navigation = [
  { id: "inicio", label: "Inicio", href: "/", icon: <Icon name="home" /> },
  ...sections.map((section) => ({
    id: section.id,
    label: section.title,
    href: `/${section.id}`,
    icon: <Icon name={section.icon} />,
  })),
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const section = getSection(pathname.split("/")[1]);
  const title =
    pathname === "/"
      ? "Inicio"
      : (section?.title ?? (pathname === "/ui" ? "Componentes" : "Página"));

  return (
    <div
      data-app-shell
      className={`${styles.shell} bg-[var(--ui-bg)] text-[var(--ui-text)]`}
      onKeyDown={(event) => {
        if (event.key === "Escape" && menuOpen) {
          setMenuOpen(false);
          document.getElementById("navigation-toggle")?.focus();
        }
      }}
    >
      <a
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:bg-[var(--ui-surface)] focus:p-4"
        href="#contenido"
      >
        Saltar al contenido
      </a>
      <header className={`${styles.header} border-b border-[var(--ui-border)] pb-4`}>
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex min-w-0 items-center gap-2">
            <Button
              id="navigation-toggle"
              variant="secondary"
              size="sm"
              className="lg:hidden"
              aria-label={menuOpen ? "Cerrar navegación" : "Abrir navegación"}
              aria-expanded={menuOpen}
              aria-controls="primary-navigation"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              <Icon name={menuOpen ? "close" : "menu"} />
            </Button>
            <nav aria-label="Ubicación actual" className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <Link href="/" className="text-sm text-[var(--ui-muted)]">
                  Espacio de trabajo
                </Link>
                <Icon name="chevron" className="shrink-0" />
                <span aria-current="page" className="text-sm font-semibold">
                  {title}
                </span>
              </div>
            </nav>
          </div>
          <Stack direction="row" align="center" gap="sm" className="ml-auto">
            <ThemeSwitcher />
            <UserProfile name="Usuario" />
          </Stack>
        </div>
      </header>
      <div
        id="primary-navigation"
        className={`${styles.navigation} ${menuOpen ? styles.navigationOpen : ""}`}
      >
        <Sidebar
          items={navigation}
          activeId={pathname === "/" ? "inicio" : section?.id}
          style={{ width: "100%", height: "100%" }}
          aria-label="Secciones principales"
          header={
            <Link href="/" onClick={() => setMenuOpen(false)}>
              <div className="flex items-center gap-2">
                <Image
                  src="/brand/gestor-rrhh.svg"
                  alt=""
                  width={32}
                  height={32}
                  className="shrink-0"
                />
                <div>
                  <strong className="block text-sm">GESTOR DE RRHH</strong>
                </div>
              </div>
            </Link>
          }
          footer={
            <p className="text-xs text-[var(--ui-muted)]">Personas, potencial y oportunidades.</p>
          }
          onItemSelect={(item, event) => {
            if (
              !item.href ||
              event.metaKey ||
              event.ctrlKey ||
              event.shiftKey ||
              event.altKey ||
              event.button !== 0
            )
              return;
            event.preventDefault();
            setMenuOpen(false);
            router.push(item.href);
          }}
        />
      </div>
      <main id="contenido" tabIndex={-1} className={styles.content}>
        {children}
      </main>
      <footer
        className={`${styles.footer} border-t border-[var(--ui-border)] py-4 text-xs text-[var(--ui-muted)]`}
      >
        <div className="flex flex-wrap justify-between gap-2">
          <span>Gestor de RRHH</span>
        </div>
      </footer>
    </div>
  );
}
