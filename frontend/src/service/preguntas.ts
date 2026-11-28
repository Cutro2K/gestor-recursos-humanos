import { apiRequest } from "@/service/api";

export type Competencia = {
  id: number;
  codigo: string;
  nombre: string;
  tipo: "TECNICA" | "ACTITUDINAL";
};

export type Factor = {
  id: number;
  competenciaId: number;
  codigo: string;
  nombre: string;
  orden: number;
};

export type ItemOpcion = {
  id: number;
  orden: number;
  descripcion: string;
};

export type OpcionRespuesta = {
  id: number;
  nombre: string;
  descripcion?: string;
  items: ItemOpcion[];
};

export type ListadoPregunta = {
  id: number;
  nombre: string;
  enunciado: string;
  factorId: number;
  nombreFactor: string;
  competenciaId: number;
  nombreCompetencia: string;
  opcionRespuestaId: number;
  nombreOpcionRespuesta: string;
  activa: boolean;
};

export type FiltrosPreguntas = {
  competenciaId?: number;
  factorId?: number;
  nombre?: string;
  enunciado?: string;
  activa?: boolean;
  offset?: number;
  cantidad?: number;
};

export type NuevaPreguntaPayload = {
  factorId: number;
  opcionRespuestaId: number;
  nombre: string;
  enunciado: string;
  descripcion?: string;
  ponderaciones: { itemOpcionId: number; ponderacion: number }[];
};

export async function listarCompetencias(): Promise<Competencia[]> {
  return (await apiRequest<Competencia[]>("/competencias")) ?? [];
}

export async function listarFactoresPorCompetencia(competenciaId: number): Promise<Factor[]> {
  return (await apiRequest<Factor[]>(`/factores/competencia/${competenciaId}`)) ?? [];
}

export async function listarOpcionesRespuesta(): Promise<OpcionRespuesta[]> {
  return (await apiRequest<OpcionRespuesta[]>("/opciones-respuesta")) ?? [];
}

export async function listarPreguntas(filtros: FiltrosPreguntas = {}): Promise<ListadoPregunta[]> {
  const params = new URLSearchParams();
  if (filtros.competenciaId) params.set("competenciaId", String(filtros.competenciaId));
  if (filtros.factorId) params.set("factorId", String(filtros.factorId));
  if (filtros.nombre?.trim()) params.set("nombre", filtros.nombre.trim());
  if (filtros.enunciado?.trim()) params.set("enunciado", filtros.enunciado.trim());
  if (filtros.activa !== undefined) params.set("activa", String(filtros.activa));
  if (filtros.offset !== undefined) params.set("offset", String(filtros.offset));
  if (filtros.cantidad !== undefined) params.set("cantidad", String(filtros.cantidad));

  const query = params.toString();
  return (await apiRequest<ListadoPregunta[]>(`/preguntas${query ? `?${query}` : ""}`)) ?? [];
}

export function crearPregunta(datos: NuevaPreguntaPayload) {
  return apiRequest<unknown>("/preguntas", { method: "POST", json: datos });
}