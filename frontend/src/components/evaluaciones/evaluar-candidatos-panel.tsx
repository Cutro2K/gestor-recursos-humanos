"use client";

import { useState } from "react";
import { PageHeader } from "@/components/ui/page-header";
import { getSection } from "@/config/sections";
import type { Candidato } from "@/service/candidatos";
import type { Puesto } from "@/service/evaluaciones";
import { PasoCandidatos } from "./paso-candidatos";
import { PasoConfirmacion } from "./paso-confirmacion";
import { PasoPuesto } from "./paso-puesto";
import styles from "./evaluaciones.module.css";

type Paso = "candidatos" | "puesto" | "confirmacion";

const PASOS: { id: Paso; titulo: string }[] = [
  { id: "candidatos", titulo: "Candidatos a evaluar" },
  { id: "puesto", titulo: "Función o puesto" },
  { id: "confirmacion", titulo: "Confirmación y claves" },
];

/**
 * CU 25 - Evaluar a Candidatos. Wizard de tres pantallas:
 * 1. Búsqueda y selección de candidatos (pasos 2 a 6 del CU).
 * 2. Selección de la función o puesto y sus competencias (pasos 7 a 10).
 * 3. Lista final, generación de claves y exportación a Excel (pasos 11 a 18).
 */
export function EvaluarCandidatosPanel() {
  const seccion = getSection("evaluaciones")!;
  const [paso, setPaso] = useState<Paso>("candidatos");
  const [aEvaluar, setAEvaluar] = useState<Candidato[]>([]);
  const [puesto, setPuesto] = useState<Puesto | null>(null);

  // Cada paso muestra el encabezado: si su carga falla, lo oculta y deja solo el error.
  const encabezado = (
    <>
      <PageHeader
        eyebrow={seccion.group}
        title={seccion.title}
        description="Elegí a los candidatos y la función o puesto para generar sus cuestionarios con una clave de ingreso para cada uno."
      />

      <ol className={styles.pasos} aria-label="Pasos de la evaluación">
        {PASOS.map((item, indice) => (
          <li key={item.id} aria-current={item.id === paso ? "step" : undefined}>
            <span className={styles.pasoNumero}>{indice + 1}</span>
            {item.titulo}
          </li>
        ))}
      </ol>
    </>
  );

  // Sin contenedor propio: así el error de un paso queda centrado en la página como en las demás secciones.
  return (
    <>
      {paso === "candidatos" && (
        <PasoCandidatos
          encabezado={encabezado}
          aEvaluar={aEvaluar}
          onCambiarAEvaluar={setAEvaluar}
          onSiguiente={() => setPaso("puesto")}
        />
      )}
      {paso === "puesto" && (
        <PasoPuesto
          encabezado={encabezado}
          puestoInicial={puesto}
          onVolver={() => setPaso("candidatos")}
          onSiguiente={(elegido) => {
            setPuesto(elegido);
            setPaso("confirmacion");
          }}
        />
      )}
      {paso === "confirmacion" && puesto && (
        <PasoConfirmacion
          encabezado={encabezado}
          puesto={puesto}
          candidatos={aEvaluar}
          onVolver={() => setPaso("puesto")}
        />
      )}
    </>
  );
}
