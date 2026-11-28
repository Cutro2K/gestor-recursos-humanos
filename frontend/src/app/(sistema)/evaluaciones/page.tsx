import type { Metadata } from "next";
import { EvaluarCandidatosPanel } from "@/components/evaluaciones/evaluar-candidatos-panel";
import { getSection } from "@/config/sections";

export const metadata: Metadata = {
  title: `${getSection("evaluaciones")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <EvaluarCandidatosPanel />;
}
