"use client";

import { createContext, useContext, useSyncExternalStore } from "react";
import type { ReactNode } from "react";
import { UIProvider } from "react-ui-lib";

type Theme = "light" | "dark" | "system";
type ThemeContextValue = {
  theme: Theme;
  resolvedTheme: "light" | "dark";
  setTheme: (theme: Theme) => void;
};

const storageKey = "capital-humano-theme";
const changeEvent = "capital-humano-theme-change";
const ThemeContext = createContext<ThemeContextValue | null>(null);
// Retain the selection in this tab when browser storage is unavailable or full.
let temporaryTheme: Theme | null = null;

function readTheme(): Theme {
  if (temporaryTheme) return temporaryTheme;
  try {
    const stored = window.localStorage.getItem(storageKey);
    return stored === "light" || stored === "dark" ? stored : "system";
  } catch {
    return "system";
  }
}

function subscribe(onChange: () => void) {
  function onStorage(event: StorageEvent) {
    if (event.key === storageKey || event.key === null) {
      temporaryTheme = null;
      onChange();
    }
  }
  window.addEventListener("storage", onStorage);
  window.addEventListener(changeEvent, onChange);
  return () => {
    window.removeEventListener("storage", onStorage);
    window.removeEventListener(changeEvent, onChange);
  };
}

function setTheme(theme: Theme) {
  temporaryTheme = theme;
  try {
    window.localStorage.setItem(storageKey, theme);
    temporaryTheme = null;
  } catch {
    // Theme switching still works when localStorage is blocked.
  }
  window.dispatchEvent(new Event(changeEvent));
}

const serverTheme = (): Theme => "system";

function subscribeSystemTheme(onChange: () => void) {
  const media = window.matchMedia("(prefers-color-scheme: dark)");
  media.addEventListener("change", onChange);
  return () => media.removeEventListener("change", onChange);
}

const readSystemDark = () => window.matchMedia("(prefers-color-scheme: dark)").matches;
const serverSystemDark = () => false;

export function ThemeProvider({ children }: { children: ReactNode }) {
  const theme = useSyncExternalStore(subscribe, readTheme, serverTheme);
  const systemDark = useSyncExternalStore(subscribeSystemTheme, readSystemDark, serverSystemDark);
  const resolvedTheme = theme === "system" ? (systemDark ? "dark" : "light") : theme;

  return (
    <ThemeContext.Provider value={{ theme, resolvedTheme, setTheme }}>
      <UIProvider theme={theme} reducedMotion="system">
        {children}
      </UIProvider>
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme debe usarse dentro de ThemeProvider.");
  return context;
}
