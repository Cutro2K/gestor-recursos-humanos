/**
 * Reglas puras de validación del formulario de alta de Opción de Respuesta (CU 29).
 * Alineadas simétricamente con las restricciones de Jakarta Bean Validation del backend.
 */
export const LIMITES_OPCION_RESPUESTA = {
  nombre: 50,
  descripcion: 1000,
  descripcionItem: 150,
  minItems: 2,
} as const;

export type ItemOpcionFormInput = {
  orden: string;
  descripcion: string;
};

export type FormularioAltaOpcionRespuesta = {
  nombre: string;
  descripcion: string;
  items: ItemOpcionFormInput[];
};

export type ErroresItemOpcion = {
  orden?: string;
  descripcion?: string;
};

export type ErroresAltaOpcionRespuesta = {
  nombre?: string;
  descripcion?: string;
  items?: string;
  filas: ErroresItemOpcion[];
};

export function crearErroresVacios(cantidadFilas: number): ErroresAltaOpcionRespuesta {
  return {
    filas: Array.from({ length: cantidadFilas }, () => ({})),
  };
}

export function sinErroresAltaOpcionRespuesta(errores: ErroresAltaOpcionRespuesta): boolean {
  return (
    !errores.nombre &&
    !errores.descripcion &&
    !errores.items &&
    errores.filas.every((fila) => !fila.orden && !fila.descripcion)
  );
}

/**
 * Calcula el próximo número de orden entero positivo sugerido al agregar una nueva fila de Ítem de Opción.
 */
export function sugerirSiguienteOrden(ordenesActuales: string[]): string {
  let maximo = 0;
  for (const texto of ordenesActuales) {
    const limpio = texto.trim();
    if (/^[1-9]\d*$/.test(limpio)) {
      const valor = Number(limpio);
      if (valor > maximo) {
        maximo = valor;
      }
    }
  }
  const base = Math.max(maximo, ordenesActuales.length);
  return String(base + 1);
}

export function validarAltaOpcionRespuesta(
  datos: FormularioAltaOpcionRespuesta,
): ErroresAltaOpcionRespuesta {
  const errores = crearErroresVacios(datos.items.length);

  const nombre = datos.nombre.trim();
  if (!nombre) {
    errores.nombre = "El nombre es obligatorio.";
  } else if (nombre.length > LIMITES_OPCION_RESPUESTA.nombre) {
    errores.nombre = `El nombre no puede superar los ${LIMITES_OPCION_RESPUESTA.nombre} caracteres.`;
  }

  const descripcion = datos.descripcion.trim();
  if (!descripcion) {
    errores.descripcion = "La descripción es obligatoria.";
  } else if (descripcion.length > LIMITES_OPCION_RESPUESTA.descripcion) {
    errores.descripcion = `La descripción no puede superar los ${LIMITES_OPCION_RESPUESTA.descripcion} caracteres.`;
  }

  if (datos.items.length < LIMITES_OPCION_RESPUESTA.minItems) {
    errores.items = "Debe ingresar al menos dos ítems de opción.";
  }

  const ordenesVistos = new Set<number>();
  const descripcionesVistas = new Set<string>();
  let todosConOrdenPositivo = datos.items.length >= LIMITES_OPCION_RESPUESTA.minItems;

  datos.items.forEach((item, idx) => {
    const ordenTexto = item.orden.trim();
    if (!ordenTexto) {
      errores.filas[idx].orden = "El orden es obligatorio.";
      todosConOrdenPositivo = false;
    } else if (!/^[1-9]\d*$/.test(ordenTexto)) {
      errores.filas[idx].orden = "El orden debe ser un entero positivo.";
      todosConOrdenPositivo = false;
    } else {
      const ordenNum = Number(ordenTexto);
      if (ordenesVistos.has(ordenNum)) {
        errores.filas[idx].orden = "El número de orden está repetido.";
      } else {
        ordenesVistos.add(ordenNum);
      }
    }

    const descItem = item.descripcion.trim();
    if (!descItem) {
      errores.filas[idx].descripcion = "La descripción del ítem de opción es obligatoria.";
    } else if (descItem.length > LIMITES_OPCION_RESPUESTA.descripcionItem) {
      errores.filas[idx].descripcion = `La descripción no puede superar los ${LIMITES_OPCION_RESPUESTA.descripcionItem} caracteres.`;
    } else {
      const clave = descItem.toLowerCase();
      if (descripcionesVistas.has(clave)) {
        errores.filas[idx].descripcion = "La descripción del ítem de opción no puede repetirse.";
      } else {
        descripcionesVistas.add(clave);
      }
    }
  });

  if (todosConOrdenPositivo) {
    const total = datos.items.length;
    const secuenciaConsecutiva =
      ordenesVistos.size === total &&
      Array.from(ordenesVistos).every((orden) => orden >= 1 && orden <= total);

    if (!secuenciaConsecutiva) {
      errores.items =
        "El orden de visualización debe ser consecutivo comenzando en 1 sin saltos ni repeticiones.";
    }
  }

  return errores;
}

/**
 * Convierte el mapa `erroresValidacion` devuelto por la API (400 / 409) en errores inline del formulario.
 */
export function mapearErroresBackendAlta(
  erroresValidacion: Record<string, string> | undefined,
  cantidadFilas: number,
): ErroresAltaOpcionRespuesta | null {
  if (!erroresValidacion || Object.keys(erroresValidacion).length === 0) {
    return null;
  }

  const resultado = crearErroresVacios(cantidadFilas);
  let asignado = false;

  for (const [campo, mensaje] of Object.entries(erroresValidacion)) {
    if (!mensaje) continue;

    if (campo === "nombre") {
      resultado.nombre = mensaje;
      asignado = true;
      continue;
    }
    if (campo === "descripcion") {
      resultado.descripcion = mensaje;
      asignado = true;
      continue;
    }
    if (campo === "items") {
      resultado.items = mensaje;
      asignado = true;
      continue;
    }

    const matchFila = /^items\[(\d+)\]\.(orden|descripcion)$/.exec(campo);
    if (matchFila) {
      const indice = Number(matchFila[1]);
      const propiedad = matchFila[2] as "orden" | "descripcion";
      if (indice >= 0 && indice < resultado.filas.length) {
        resultado.filas[indice][propiedad] = mensaje;
        asignado = true;
      }
    }
  }

  return asignado ? resultado : null;
}
