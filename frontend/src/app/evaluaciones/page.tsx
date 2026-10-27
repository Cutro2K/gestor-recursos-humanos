import type { Metadata } from "next";
import { SectionOverview } from "@/components/layout/section-overview";
import { getSection } from "@/config/sections";

export const metadata: Metadata = {
  title: `${getSection("evaluaciones")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <SectionOverview id="evaluaciones" />;
}
