import { apiRequest } from "./api";

export type FiltrosOpcionesRespuesta = {
  nombre: string;
};

export type ItemOpcion = {
  id?: number | null;
  orden: number;
  descripcion: string;
};

export type ListadoOpcionRespuestaDTO = {
  id: number;
  nombre: string;
  descripcion: string;
};

export type OpcionRespuestaDetalle = ListadoOpcionRespuestaDTO & {
  items: ItemOpcion[];
};

export type ListadoOpcionesRespuesta = {
  opciones: ListadoOpcionRespuestaDTO[];
  siguienteCursor: number | null;
};

export type CrearOpcionRespuestaPayload = {
  nombre: string;
  descripcion: string;
  items: Array<{
    orden: number;
    descripcion: string;
  }>;
};

const CANTIDAD_POR_PAGINA = 50;

/**
 * Busca opciones de respuesta activas filtrando opcionalmente por nombre (paginado por offset/cantidad).
 * Endpoint: GET /api/opciones-respuesta (CU 24)
 */
export async function listarOpcionesRespuesta(
  filtros: FiltrosOpcionesRespuesta,
  cursor: number | null,
  signal?: AbortSignal,
): Promise<ListadoOpcionesRespuesta> {
  const offset = cursor !== null && cursor >= 0 ? cursor : 0;
  const query = new URLSearchParams({
    offset: String(offset),
    cantidad: String(CANTIDAD_POR_PAGINA),
  });

  if (filtros.nombre.trim()) {
    query.set("nombre", filtros.nombre.trim());
  }

  const resultado = await apiRequest<ListadoOpcionRespuestaDTO[]>(
    `opciones-respuesta?${query}`,
    { signal },
  );

  if (!resultado) {
    throw new Error("No recibimos el listado de opciones de respuesta.");
  }

  const opciones: ListadoOpcionRespuestaDTO[] = resultado.map((item) => ({
    id: item.id,
    nombre: item.nombre,
    descripcion: item.descripcion,
  }));

  const siguienteCursor =
    opciones.length === CANTIDAD_POR_PAGINA ? offset + opciones.length : null;

  return { opciones, siguienteCursor };
}

/**
 * Obtiene el detalle de una opción de respuesta activa por su ID, incluyendo sus ítems de opción ordenados.
 * Endpoint: GET /api/opciones-respuesta/{id} (CU 21 / CU 22 / CU 30)
 */
export async function obtenerOpcionRespuestaPorId(
  id: number,
  signal?: AbortSignal,
): Promise<OpcionRespuestaDetalle> {
  const resultado = await apiRequest<OpcionRespuestaDetalle>(
    `opciones-respuesta/${id}`,
    { signal },
  );
  if (!resultado) {
    throw new Error(`No se encontró la opción de respuesta #${id}.`);
  }
  return resultado;
}

/**
 * Da de alta una nueva opción de respuesta con su lista de ítems de opción.
 * Endpoint: POST /api/opciones-respuesta (CU 29)
 */
export async function crearOpcionRespuesta(
  datos: CrearOpcionRespuestaPayload,
  signal?: AbortSignal,
): Promise<OpcionRespuestaDetalle> {
  const resultado = await apiRequest<OpcionRespuestaDetalle>("opciones-respuesta", {
    method: "POST",
    json: datos,
    signal,
  });
  if (!resultado) {
    throw new Error("No se pudo registrar la opción de respuesta.");
  }
  return resultado;
}
