"use client";

import { createContext, useContext, useMemo, useSyncExternalStore } from "react";
import type { ReactNode } from "react";
import type { Sesion } from "@/service/auth";
import { SESSION_COOKIE } from "./session-cookie";

/** Sesión del backend más el nombre que se muestra en el avatar. */
export type UserSession = Sesion & { nombre: string };

/** Por ahora entrar como invitado equivale a estar autenticado. */
export const guestSession: UserSession = { sujetoId: null, rol: "INVITADO", nombre: "Invitado" };

type SessionContextValue = {
  session: UserSession | null;
  signIn: (session: UserSession) => void;
  signOut: () => void;
};

const changeEvent = "gestor-rrhh-sesion-change";
const SessionContext = createContext<SessionContextValue | null>(null);

// La sesión vive en una cookie para que src/proxy.ts pueda leerla en el servidor.
function readCookie() {
  const prefix = `${SESSION_COOKIE}=`;
  const entry = document.cookie.split("; ").find((cookie) => cookie.startsWith(prefix));
  return entry ? decodeURIComponent(entry.slice(prefix.length)) : null;
}

function save(session: UserSession | null) {
  document.cookie = session
    ? `${SESSION_COOKIE}=${encodeURIComponent(JSON.stringify(session))}; path=/; SameSite=Lax`
    : `${SESSION_COOKIE}=; path=/; max-age=0`;
  window.dispatchEvent(new Event(changeEvent));
}

function subscribe(onChange: () => void) {
  window.addEventListener(changeEvent, onChange);
  return () => window.removeEventListener(changeEvent, onChange);
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const stored = useSyncExternalStore(subscribe, readCookie, () => null);
  const value = useMemo<SessionContextValue>(
    () => ({
      session: stored ? (JSON.parse(stored) as UserSession) : null,
      signIn: save,
      signOut: () => save(null),
    }),
    [stored],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error("useSession debe usarse dentro de SessionProvider.");
  return context;
}
