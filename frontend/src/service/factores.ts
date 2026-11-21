import { apiRequest } from "./api";

export type CompetenciaResumen = {
  id: number;
  nombre: string;
};

export type FiltrosFactores = {
  nombreCompetencia: string;
  nombreFactor: string;
};

export type Factor = {
  id?: number | null;
  competenciaId: number;
  nombreCompetencia?: string;
  codigo: string;
  nombre: string;
  orden: number;
  descripcion?: string | null;
};

export type ListadoGestorFactoresDTO = {
  id: number;
  codigo: string;
  nombreFactor: string;
  competenciaId: number;
  nombreCompetencia: string;
  orden: number;
  descripcion?: string | null;
};

export type ListadoFactor = {
  factores: Factor[];
  siguienteCursor: number | null;
};

export type CrearFactorPayload = {
  competenciaId: number;
  codigo: string;
  nombre: string;
  orden: number;
  descripcion?: string | null;
};

export type ModificarFactorPayload = CrearFactorPayload;

const CANTIDAD_POR_PAGINA = 50;

/**
 * Consulta las competencias activas disponibles para asociar a un factor.
 * Endpoint: GET /api/competencias
 */
export async function listarCompetenciasDisponibles(
  signal?: AbortSignal
): Promise<CompetenciaResumen[]> {
  const resultado = await apiRequest<CompetenciaResumen[]>("competencias", { signal });
  return resultado ?? [];
}

/**
 * Busca factores activos filtrando por nombre de competencia y/o nombre de factor (paginado por offset/cantidad).
 * Endpoint: GET /api/factores (CU 16 / CU 20)
 */
export async function listarFactores(
  filtros: FiltrosFactores,
  cursor: number | null,
  signal?: AbortSignal
): Promise<ListadoFactor> {
  const offset = cursor !== null && cursor >= 0 ? cursor : 0;
  const query = new URLSearchParams({
    offset: String(offset),
    cantidad: String(CANTIDAD_POR_PAGINA),
  });

  if (filtros.nombreCompetencia.trim()) {
    query.set("nombreCompetencia", filtros.nombreCompetencia.trim());
  }
  if (filtros.nombreFactor.trim()) {
    query.set("nombreFactor", filtros.nombreFactor.trim());
  }

  const resultado = await apiRequest<
    Array<ListadoGestorFactoresDTO & Partial<Factor>> | ListadoFactor
  >(`factores?${query}`, { signal });

  if (!resultado) throw new Error("No recibimos el listado de factores.");

  if (Array.isArray(resultado)) {
    const factores: Factor[] = resultado.map((item) => ({
      id: item.id,
      competenciaId: item.competenciaId,
      nombreCompetencia: item.nombreCompetencia,
      codigo: item.codigo,
      nombre: item.nombre ?? item.nombreFactor ?? "",
      orden: item.orden,
      descripcion: item.descripcion ?? null,
    }));

    const siguienteCursor =
      factores.length === CANTIDAD_POR_PAGINA ? offset + factores.length : null;

    return { factores, siguienteCursor };
  }

  return resultado;
}

/**
 * Obtiene el detalle de un factor activo por su ID.
 * Endpoint: GET /api/factores/{id} (CU 18)
 */
export async function obtenerFactorPorId(
  id: number,
  signal?: AbortSignal
): Promise<Factor> {
  const resultado = await apiRequest<Factor>(`factores/${id}`, { signal });
  if (!resultado) throw new Error(`No se encontró el factor #${id}.`);
  return resultado;
}

/**
 * Lista todos los factores activos asociados a una competencia, ordenados por orden.
 * Endpoint: GET /api/factores/competencia/{competenciaId} (CU 21 / CU 22)
 */
export async function listarFactoresPorCompetencia(
  competenciaId: number,
  signal?: AbortSignal
): Promise<Factor[]> {
  const resultado = await apiRequest<Factor[]>(`factores/competencia/${competenciaId}`, {
    signal,
  });
  return resultado ?? [];
}

/**
 * Da de alta un nuevo factor en el sistema.
 * Endpoint: POST /api/factores (CU 17)
 */
export async function crearFactor(
  datos: CrearFactorPayload,
  signal?: AbortSignal
): Promise<Factor> {
  const resultado = await apiRequest<Factor>("factores", {
    method: "POST",
    json: datos,
    signal,
  });
  if (!resultado) throw new Error("No se pudo registrar el factor.");
  return resultado;
}

/**
 * Modifica los datos de un factor existente.
 * Endpoint: PUT /api/factores/{id} (CU 18)
 */
export async function modificarFactor(
  id: number,
  datos: ModificarFactorPayload,
  signal?: AbortSignal
): Promise<Factor> {
  const resultado = await apiRequest<Factor>(`factores/${id}`, {
    method: "PUT",
    json: datos,
    signal,
  });
  if (!resultado) throw new Error("No se pudo modificar el factor.");
  return resultado;
}

/**
 * Realiza la baja lógica de un factor.
 * Endpoint: DELETE /api/factores/{id} (CU 19)
 */
export async function eliminarFactor(id: number, signal?: AbortSignal): Promise<void> {
  await apiRequest<void>(`factores/${id}`, {
    method: "DELETE",
    signal,
  });
}

/**
 * Realiza la baja lógica de múltiples factores.
 * Endpoint: DELETE /api/factores/{id} (CU 19)
 */
export async function eliminarFactores(ids: number[], signal?: AbortSignal): Promise<void> {
  await Promise.all(
    ids.map((id) =>
      apiRequest<void>(`factores/${id}`, {
        method: "DELETE",
        signal,
      })
    )
  );
}
