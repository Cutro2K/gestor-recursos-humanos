import { apiRequest } from "./api";

export interface Puesto {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
  descripcion?: string | null;
}

export interface EvaluacionRequest {
  puestoId: number;
  candidatoIds: number[];
  versionModelo?: string;
}

export interface CuestionarioGenerado {
  cuestionarioId: number;
  candidatoId: number;
  candidatoNombre: string;
  candidatoApellido: string;
  claveIngreso: string;
  estado: string;
  cantidadBloques: number;
  cantidadPreguntas: number;
}

export interface EvaluacionResponse {
  evaluacionId: number;
  puestoId: number;
  puestoNombre: string;
  fechaCreacion: string;
  cantidadCuestionarios: number;
  cuestionarios: CuestionarioGenerado[];
}

export async function listarPuestos(signal?: AbortSignal): Promise<Puesto[]> {
  const data = await apiRequest<Puesto[]>("evaluaciones/puestos", { signal });
  return data ?? [];
}

export async function generarEvaluacion(
  payload: EvaluacionRequest,
): Promise<EvaluacionResponse> {
  const data = await apiRequest<EvaluacionResponse>("evaluaciones", {
    method: "POST",
    json: payload,
  });
  if (!data) throw new Error("No recibimos el resultado de la evaluación.");
  return data;
}
