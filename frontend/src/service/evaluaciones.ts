import { API_BASE_URL, ApiError, apiRequest } from "./api";

/** Puesto que se puede elegir para evaluar (CU 25, paso 8). */
export type Puesto = {
  id: number;
  codigo: string;
  nombre: string;
  empresa: string;
};

/** Competencia del puesto con su ponderación; evaluable = false dispara el flujo 9.A. */
export type CompetenciaPuesto = {
  competenciaId: number;
  codigo: string;
  nombre: string;
  ponderacionMinima: number;
  evaluable: boolean;
};

export type CredencialCuestionario = {
  candidatoId: number;
  apellido: string;
  nombre: string;
  tipoDocumento: string;
  nroDocumento: string;
  claveIngreso: string;
};

export type EvaluacionResultado = {
  evaluacionId: number;
  puestoId: number;
  puestoNombre: string;
  empresa: string;
  fechaCreacion: string;
  nombreArchivoExcel: string;
  credenciales: CredencialCuestionario[];
};

export async function listarPuestos(signal?: AbortSignal): Promise<Puesto[]> {
  return (await apiRequest<Puesto[]>("evaluaciones/puestos", { signal })) ?? [];
}

export async function consultarCompetenciasPuesto(
  puestoId: number,
  signal?: AbortSignal,
): Promise<CompetenciaPuesto[]> {
  return (
    (await apiRequest<CompetenciaPuesto[]>(`evaluaciones/puestos/${puestoId}/competencias`, {
      signal,
    })) ?? []
  );
}

/** Paso 6: resuelve si todos pueden evaluarse; un 409 trae quiénes tienen cuestionarios abiertos (6.A). */
export async function verificarCandidatos(candidatoIds: number[], signal?: AbortSignal) {
  await apiRequest("evaluaciones/verificacion-candidatos", {
    method: "POST",
    json: { candidatoIds },
    signal,
  });
}

export async function evaluarCandidatos(
  puestoId: number,
  candidatoIds: number[],
): Promise<EvaluacionResultado> {
  const resultado = await apiRequest<EvaluacionResultado>("evaluaciones", {
    method: "POST",
    json: { puestoId, candidatoIds },
  });
  if (!resultado) throw new Error("No recibimos el resultado de la evaluación.");
  return resultado;
}

/** Pasos 15 a 17: descarga el Excel (.xls) con los candidatos evaluados y sus claves. */
export async function descargarClavesExcel(evaluacionId: number, signal?: AbortSignal): Promise<Blob> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}/evaluaciones/${evaluacionId}/claves`, {
      cache: "no-store",
      credentials: "include",
      signal,
    });
  } catch (error) {
    if (signal?.aborted) throw error;
    throw new ApiError("No pudimos conectar con el servidor. Intentá nuevamente.", 0);
  }

  if (!response.ok) {
    let message = "No pudimos generar el archivo Excel. Intentá nuevamente.";
    try {
      const detalle = (await response.json()) as { message?: unknown };
      if (typeof detalle.message === "string" && detalle.message.trim()) message = detalle.message;
    } catch {
      // La respuesta de error no era JSON: queda el mensaje genérico.
    }
    throw new ApiError(message, response.status);
  }
  return response.blob();
}
