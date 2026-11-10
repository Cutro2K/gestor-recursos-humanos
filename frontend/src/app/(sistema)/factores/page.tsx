import type { Metadata } from "next";
import { getSection } from "@/config/sections";
import { FactoresPanel } from "@/components/factores/factores-panel";

export const metadata: Metadata = {
  title: `${getSection("factores")!.title} | Capit@l Humano`,
};

export default function Page() {
  return <FactoresPanel/>;
}
