import type { Metadata } from "next";
import { EvaluarCandidatosPanel } from "@/components/evaluaciones/evaluar-candidatos-panel";

export const metadata: Metadata = {
  title: "Evaluar candidatos | Capit@l Humano",
};

export default function Page() {
  return <EvaluarCandidatosPanel />;
}
