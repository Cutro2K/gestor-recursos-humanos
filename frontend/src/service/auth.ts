import { apiRequest } from "./api";

// Contratos de AuthController (CU-01 Autenticar usuario).
export type Sesion = { sujetoId: number | null; rol: string };

export const tiposDocumento = ["DNI", "LE", "LC", "PP"] as const;
export type TipoDocumento = (typeof tiposDocumento)[number];

export async function loginConsultor(usuario: string, contrasena: string) {
  return (await apiRequest<Sesion>("auth/consultor", {
    method: "POST",
    json: { usuario, contrasena },
  })) as Sesion;
}

export async function loginCandidato(
  tipoDocumento: TipoDocumento,
  nroDocumento: string,
  clave: string,
) {
  return (await apiRequest<Sesion>("auth/candidato", {
    method: "POST",
    json: { tipoDocumento, nroDocumento, clave },
  })) as Sesion;
}
