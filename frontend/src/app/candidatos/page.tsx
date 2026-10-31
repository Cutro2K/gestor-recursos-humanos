import type { Metadata } from "next";
import { CandidatosPanel } from "@/components/candidatos/candidatos-panel";
import { getSection } from "@/config/sections";

export const metadata: Metadata = {
  title: `${getSection("candidatos")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <CandidatosPanel />;
}
