import type { Metadata } from "next";
import { NuevaEvaluacionPanel } from "@/components/evaluaciones/nueva-evaluacion-panel";

export const metadata: Metadata = {
  title: "Nueva evaluación | Capit@l Humano",
};

export default function Page() {
  return <NuevaEvaluacionPanel />;
}
