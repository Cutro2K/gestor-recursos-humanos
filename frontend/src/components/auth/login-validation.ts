import type { TipoDocumento } from "@/service/auth";

/**
 * Validación del formulario de login (CU-01), antes de llamar a la API.
 * Los límites siguen db/01-schema.sql y los DTO de la API:
 * consultor.username VARCHAR(30), candidato.nro_documento VARCHAR(20),
 * cuestionario.clave_ingreso CHAR(8).
 */
export const LIMITES = {
  usuario: 30,
  contrasena: 128,
  nroDocumento: 20,
  clave: 8,
} as const;

export type ErroresConsultor = { usuario?: string; contrasena?: string };
export type ErroresCandidato = { nroDocumento?: string; clave?: string };

/** DNI, LE y LC se escriben a veces con puntos o espacios ("40.123.456"): se quitan. */
export function normalizarDocumento(tipo: TipoDocumento, valor: string) {
  const limpio = valor.trim();
  return tipo === "PP" ? limpio.toUpperCase() : limpio.replace(/[.\s]/g, "");
}

export function validarConsultor(usuario: string, contrasena: string): ErroresConsultor {
  const errores: ErroresConsultor = {};
  const nombre = usuario.trim();

  if (!nombre) errores.usuario = "Ingresá tu usuario.";
  else if (nombre.length > LIMITES.usuario)
    errores.usuario = `Máximo ${LIMITES.usuario} caracteres.`;

  if (!contrasena) errores.contrasena = "Ingresá tu contraseña.";
  else if (contrasena.length > LIMITES.contrasena)
    errores.contrasena = `Máximo ${LIMITES.contrasena} caracteres.`;

  return errores;
}

export function validarCandidato(
  tipo: TipoDocumento,
  nroDocumento: string,
  clave: string,
): ErroresCandidato {
  const errores: ErroresCandidato = {};
  const documento = normalizarDocumento(tipo, nroDocumento);

  if (!documento) errores.nroDocumento = "Ingresá tu número de documento.";
  else if (tipo === "PP") {
    if (!/^[A-Z0-9]+$/.test(documento))
      errores.nroDocumento = "El pasaporte solo puede tener letras y números.";
    else if (documento.length > LIMITES.nroDocumento)
      errores.nroDocumento = `Máximo ${LIMITES.nroDocumento} caracteres.`;
  } else if (!/^\d{7,8}$/.test(documento)) {
    errores.nroDocumento = "Debe tener 7 u 8 números.";
  }

  if (!clave) errores.clave = "Ingresá la clave del cuestionario.";
  else if (!/^[A-Za-z0-9]{8}$/.test(clave))
    errores.clave = `Debe tener exactamente ${LIMITES.clave} letras o números.`;

  return errores;
}

export function sinErrores(errores: object) {
  return Object.keys(errores).length === 0;
}
