import { apiRequest } from "./api";

export type CompetenciaResumen = {
  id: number;
  nombre: string;
};

export const COMPETENCIAS_MOCK: CompetenciaResumen[] = [
  { id: 1, nombre: "Gestión de proyectos" },
  { id: 2, nombre: "Liderazgo y comunicación" },
  { id: 3, nombre: "Resolución de problemas" },
];

let FACTORES_MOCK: Factor[] = [
  {
    id: 1,
    competenciaId: 1,
    nombreCompetencia: "Gestión de proyectos",
    codigo: "FAC-01",
    nombre: "Capacidad de análisis",
    orden: 1,
    descripcion: "Pensamiento lógico y deductivo",
  },
  {
    id: 2,
    competenciaId: 1,
    nombreCompetencia: "Gestión de proyectos",
    codigo: "FAC-02",
    nombre: "Atención al detalle",
    orden: 2,
    descripcion: "Minuciosidad y precision de tareas",
  },
  {
    id: 3,
    competenciaId: 2,
    nombreCompetencia: "Liderazgo y comunicación",
    codigo: "FAC-03",
    nombre: "Trabajo en equipo",
    orden: 1,
    descripcion: "Colaboración activa y comunicación",
  },
];

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

export type ListadoFactor = {
  factores: Factor[];
  siguienteCursor: number | null;
};

export async function listarCompetenciasDisponibles(
  signal?: AbortSignal
): Promise<CompetenciaResumen[]> {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  const resultado = await apiRequest<CompetenciaResumen[]>("competencias", { signal });
  return resultado ?? [];
  --------------------------------------------- */
  void signal;
  return COMPETENCIAS_MOCK;
}

export async function listarFactores(
  filtros: FiltrosFactores,
  cursor: number | null,
  signal?: AbortSignal
): Promise<ListadoFactor> {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  const query = new URLSearchParams({ limite: "50" });
  for (const [campo, valor] of Object.entries(filtros)) {
    if (valor.trim()) query.set(campo, valor.trim());
  }
  if (cursor !== null) query.set("cursor", String(cursor));
  const resultado = await apiRequest<ListadoFactor | Factor[]>(`factores?${query}`, { signal });
  if (!resultado) throw new Error("No recibimos el listado de factores.");
  if (Array.isArray(resultado)) {
    const ordenados = [...resultado].sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
    return { factores: ordenados, siguienteCursor: null };
  }
  return resultado;
  --------------------------------------------- */

  void apiRequest;
  void cursor;
  void signal;
  await new Promise((resolve) => setTimeout(resolve, 200));

  const compBuscada = filtros.nombreCompetencia.trim().toLowerCase();
  const facBuscado = filtros.nombreFactor.trim().toLowerCase();

  const filtrados = FACTORES_MOCK.filter((factor) => {
    if (
      compBuscada &&
      !(factor.nombreCompetencia ?? "").toLowerCase().includes(compBuscada)
    ) {
      return false;
    }
    if (facBuscado && !factor.nombre.toLowerCase().includes(facBuscado)) {
      return false;
    }
    return true;
  }).sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));

  return { factores: filtrados, siguienteCursor: null };
}

export type CrearFactorPayload = {
  competenciaId: number;
  codigo: string;
  nombre: string;
  orden: number;
  descripcion?: string | null;
};

export type ModificarFactorPayload = CrearFactorPayload;

export async function crearFactor(datos: CrearFactorPayload, signal?: AbortSignal) {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  const resultado = await apiRequest<Factor>("factores", {
    method: "POST",
    json: datos,
    signal,
  });
  if (!resultado) throw new Error("No se pudo registrar el factor.");
  return resultado;
  --------------------------------------------- */

  void signal;
  await new Promise((resolve) => setTimeout(resolve, 300));

  // Validaciones CU-017: 2.B (código y/o nombre ya existe) y 2.C (nro de orden ya existe en la competencia)
  if (FACTORES_MOCK.some((f) => f.codigo.toUpperCase() === datos.codigo.toUpperCase())) {
    throw new Error(`El código "${datos.codigo}" ya existe en el sistema.`);
  }
  if (FACTORES_MOCK.some((f) => f.nombre.toUpperCase() === datos.nombre.toUpperCase())) {
    throw new Error(`El nombre de factor "${datos.nombre}" ya existe en el sistema.`);
  }
  if (
    FACTORES_MOCK.some(
      (f) => f.competenciaId === datos.competenciaId && f.orden === datos.orden
    )
  ) {
    throw new Error(
      `El número de orden ${datos.orden} ya existe para la competencia seleccionada.`
    );
  }

  const competencia = COMPETENCIAS_MOCK.find((c) => c.id === datos.competenciaId);
  const nuevoFactor: Factor = {
    id: Date.now(),
    ...datos,
    nombreCompetencia: competencia?.nombre ?? `Competencia #${datos.competenciaId}`,
  };
  FACTORES_MOCK.push(nuevoFactor);
  return nuevoFactor;
}

export async function modificarFactor(
  id: number,
  datos: ModificarFactorPayload,
  signal?: AbortSignal
): Promise<Factor> {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  const resultado = await apiRequest<Factor>(`factores/${id}`, {
    method: "PUT",
    json: datos,
    signal,
  });
  if (!resultado) throw new Error("No se pudo modificar el factor.");
  return resultado;
  --------------------------------------------- */

  void signal;
  await new Promise((resolve) => setTimeout(resolve, 300));
  const indice = FACTORES_MOCK.findIndex((f) => f.id === id);
  if (indice === -1) {
    throw new Error("No se encontró el factor a modificar.");
  }

  // Validaciones CU-018: 2.B y 2.C
  if (
    FACTORES_MOCK.some(
      (f) => f.id !== id && f.nombre.toUpperCase() === datos.nombre.toUpperCase()
    )
  ) {
    throw new Error(`El nombre de factor "${datos.nombre}" ya existe en el sistema.`);
  }
  if (
    FACTORES_MOCK.some(
      (f) =>
        f.id !== id &&
        f.competenciaId === datos.competenciaId &&
        f.orden === datos.orden
    )
  ) {
    throw new Error(
      `El número de orden ${datos.orden} ya existe para la competencia seleccionada.`
    );
  }

  const competencia = COMPETENCIAS_MOCK.find((c) => c.id === datos.competenciaId);
  const actualizado: Factor = {
    ...FACTORES_MOCK[indice],
    ...datos,
    id,
    nombreCompetencia: competencia?.nombre ?? `Competencia #${datos.competenciaId}`,
  };
  FACTORES_MOCK[indice] = actualizado;
  return actualizado;
}

export async function eliminarFactor(id: number, signal?: AbortSignal): Promise<void> {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  await apiRequest<void>(`factores/${id}`, {
    method: "DELETE",
    signal,
  });
  --------------------------------------------- */

  void signal;
  await new Promise((resolve) => setTimeout(resolve, 250));
  FACTORES_MOCK = FACTORES_MOCK.filter((f) => f.id !== id);
}

export async function eliminarFactores(ids: number[], signal?: AbortSignal): Promise<void> {
  /* --- DESCOMENTAR AL INTEGRAR CON BACKEND ---
  await Promise.all(
    ids.map((id) =>
      apiRequest<void>(`factores/${id}`, {
        method: "DELETE",
        signal,
      })
    )
  );
  --------------------------------------------- */

  void signal;
  await new Promise((resolve) => setTimeout(resolve, 300));
  const conjunto = new Set(ids);
  FACTORES_MOCK = FACTORES_MOCK.filter((f) => f.id == null || !conjunto.has(f.id));
}
