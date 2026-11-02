import type { Metadata } from "next";
import { LoginScreen } from "@/components/auth/login-screen";

export const metadata: Metadata = {
  title: "Iniciar sesión | Capit@l Humano",
};

export default function Page() {
  return <LoginScreen />;
}
