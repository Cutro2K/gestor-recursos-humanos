import type { Metadata } from "next";
import "react-ui-lib/styles.css";
import "./globals.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "Gestor de Recursos Humanos",
  description: "Gestión de puestos, competencias y evaluación de candidatos.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="es">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
