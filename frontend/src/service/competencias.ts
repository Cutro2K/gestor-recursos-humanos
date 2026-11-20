import { apiRequest } from "./api";

export type TipoCompetencia = "TECNICA" | "ACTITUDINAL";

export type Competencia = {
  id: number;
  codigo: string;
  nombre: string;
  tipo: TipoCompetencia;
  descripcion?: string | null;
  eliminada?: boolean;
};

export type CrearCompetenciaInput = {
  codigo: string;
  nombre: string;
  tipo: TipoCompetencia;
  descripcion?: string;
};

export type FiltrosCompetencias = {
  nombre?: string;
  codigo?: string;
  tipo?: TipoCompetencia | "";
};

/**
 * Consulta el listado de competencias activas con filtros opcionales.
 */
export async function listarCompetencias(
  filtros?: FiltrosCompetencias,
  signal?: AbortSignal,
): Promise<Competencia[]> {
  const query = new URLSearchParams();

  if (filtros?.nombre?.trim()) query.set("nombre", filtros.nombre.trim());
  if (filtros?.codigo?.trim()) query.set("codigo", filtros.codigo.trim());
  if (filtros?.tipo?.trim()) query.set("tipo", filtros.tipo.trim());

  const queryString = query.toString();
  const ruta = queryString ? `competencias?${queryString}` : "competencias";

  const resultado = await apiRequest<Competencia[]>(ruta, { signal });
  return resultado ?? [];
}

/**
 * Obtiene el detalle de una competencia por su ID.
 */
export async function obtenerCompetencia(
  id: number,
  signal?: AbortSignal,
): Promise<Competencia> {
  const resultado = await apiRequest<Competencia>(`competencias/${id}`, { signal });
  if (!resultado) {
    throw new Error(`No se encontró la competencia #${id}.`);
  }
  return resultado;
}

/**
 * Registra una nueva competencia en el sistema (CU 13).
 */
export async function crearCompetencia(
  datos: CrearCompetenciaInput,
  signal?: AbortSignal,
): Promise<Competencia> {
  const payload = {
    codigo: datos.codigo.trim(),
    nombre: datos.nombre.trim(),
    tipo: datos.tipo,
    descripcion: datos.descripcion?.trim() || undefined,
  };

  const resultado = await apiRequest<Competencia>("competencias", {
    method: "POST",
    json: payload,
    signal,
  });

  if (!resultado) {
    throw new Error("No se recibió la confirmación de la competencia creada.");
  }

  return resultado;
}
