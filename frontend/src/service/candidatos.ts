import { apiRequest } from "./api";

export const COLUMNAS_CSV = [
  "Nro candidato", "Apellido", "Nombre", "Nro documento", "Tipo documento",
  "Fecha de nacimiento", "Género", "Nacionalidad", "Email", "Escolaridad", "Eliminado",
] as const;

export const MAX_CSV_BYTES = 2 * 1024 * 1024;

export type AccionImportacion = "CREAR" | "ACTUALIZAR" | "ELIMINAR" | "RECHAZAR";

export type ErrorFila = { fila: number; campo: string; mensaje: string };

export type ResumenErrores = {
  filasConErrores: number;
  filasPorCampo: Record<string, number>;
};

export type FilaPrevisualizacion = {
  fila: number;
  valores: Record<string, string>;
  accion: AccionImportacion;
  errores: ErrorFila[];
};

export type Previsualizacion = {
  filas: FilaPrevisualizacion[];
  aCrear: number;
  aActualizar: number;
  aEliminar: number;
  rechazadas: number;
  resumenErrores: ResumenErrores;
};

export type ResultadoImportacion = {
  creados: number;
  actualizados: number;
  eliminados: number;
  rechazados: number;
  errores: ErrorFila[];
  resumenErrores: ResumenErrores;
};

export type Candidato = {
  id: number;
  nroCandidato: number;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  email: string;
};

export type ListadoCandidatos = { candidatos: Candidato[]; siguienteCursor: number | null };

export type FiltrosCandidatos = { apellido: string; nombre: string; nroCandidato: string };

export async function listarCandidatos(
  filtros: FiltrosCandidatos,
  cursor: number | null,
  signal?: AbortSignal,
): Promise<ListadoCandidatos> {
  const query = new URLSearchParams({ limite: "50" });

  for (const [campo, valor] of Object.entries(filtros)) {
    if (valor.trim()) query.set(campo, valor.trim());
  }

  if (cursor !== null) query.set("cursor", String(cursor));

  const resultado = await apiRequest<ListadoCandidatos>(`candidatos?${query}`, { signal });
  if (!resultado) throw new Error("No recibimos el listado de candidatos.");

  return resultado;
}

async function enviarCsv<T>(ruta: string, archivo: File, signal?: AbortSignal): Promise<T> {
  const body = new FormData();
  body.set("archivo", archivo);

  const resultado = await apiRequest<T>(`candidatos/${ruta}`, { method: "POST", body, signal });
  if (!resultado) throw new Error("No recibimos el resultado de la operación.");

  return resultado;
}

export function previsualizarCandidatos(archivo: File, signal?: AbortSignal) {
  return enviarCsv<Previsualizacion>("previsualizar", archivo, signal);
}

export function importarCandidatos(archivo: File, signal?: AbortSignal) {
  return enviarCsv<ResultadoImportacion>("importar", archivo, signal);
}
