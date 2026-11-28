import type { Metadata } from "next";
import { OpcionesRespuestaPanel } from "@/components/opciones-respuesta/opciones-respuesta-panel";
import { getSection } from "@/config/sections";

export const metadata: Metadata = {
  title: `${getSection("opciones-respuesta")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <OpcionesRespuestaPanel />;
}
