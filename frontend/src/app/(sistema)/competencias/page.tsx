import type { Metadata } from "next";
import { CompetenciasPanel } from "@/components/competencias/competencias-panel";
import { getSection } from "@/config/sections";

export const metadata: Metadata = {
  title: `${getSection("competencias")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <CompetenciasPanel />;
}
