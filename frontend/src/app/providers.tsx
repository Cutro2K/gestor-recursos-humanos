"use client";

import type { ReactNode } from "react";
import { ToastProvider } from "react-ui-lib";
import { SessionProvider } from "@/components/auth/session-provider";
import { ThemeProvider } from "@/components/theme/theme-provider";

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <ToastProvider position="bottom-right" maxVisibleToasts={3} viewportLabel="Notificaciones">
        <SessionProvider>{children}</SessionProvider>
      </ToastProvider>
    </ThemeProvider>
  );
}
