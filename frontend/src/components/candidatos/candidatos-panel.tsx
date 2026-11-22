"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Button, Spinner, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { CriteriosBusqueda } from "@/components/ui/criterios-busqueda";
import type { CriterioAplicado } from "@/components/ui/criterios-busqueda";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { listarCandidatos } from "@/service/candidatos";
import type { Candidato, FiltrosCandidatos } from "@/service/candidatos";
import { ImportarCandidatosModal } from "./importar-candidatos-modal";
import styles from "./candidatos.module.css";

const FILTROS_VACIOS: FiltrosCandidatos = { apellido: "", nombre: "", nroCandidato: "" };

const ETIQUETAS_CRITERIO: Record<keyof FiltrosCandidatos, string> = {
  apellido: "Apellido",
  nombre: "Nombre",
  nroCandidato: "N.º",
};

function normalizar(filtros: FiltrosCandidatos): FiltrosCandidatos {
  return {
    apellido: filtros.apellido.trim(),
    nombre: filtros.nombre.trim(),
    nroCandidato: filtros.nroCandidato.trim(),
  };
}

export function CandidatosPanel() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [consulta, setConsulta] = useState({ filtros: FILTROS_VACIOS, revision: 0 });
  const [candidatos, setCandidatos] = useState<Candidato[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [cargandoMas, setCargandoMas] = useState(false);
  const [error, setError] = useState(false);
  const toast = useToast();
  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargando || cargandoMas,
  });
  const siguientePeticion = useRef<AbortController | null>(null);
  const hayFiltros = Object.values(consulta.filtros).some((valor) => valor.trim());
  const hayFiltrosEscritos = Object.values(filtros).some((valor) => valor.trim());

  const criteriosAplicados: CriterioAplicado[] = (["apellido", "nombre", "nroCandidato"] as const)
    .filter((criterio) => consulta.filtros[criterio])
    .map((criterio) => ({
      clave: criterio,
      etiqueta: ETIQUETAS_CRITERIO[criterio],
      valor: criterio === "nroCandidato" ? consulta.filtros[criterio] : `“${consulta.filtros[criterio]}”`,
      onQuitar: () => quitarCriterio(criterio),
    }));

  const recargar = useCallback(() => {
    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ ...anterior, revision: anterior.revision + 1 }));
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    void listarCandidatos(consulta.filtros, null, controller.signal)
      .then((resultado) => {
        if (controller.signal.aborted) return;
        setCandidatos(resultado.candidatos);
        setCursor(resultado.siguienteCursor);
        clearError();
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setCandidatos([]);
        setCursor(null);
        setError(true);
        reportError(error, {
          title: "No pudimos completar la consulta",
          fallback: "No pudimos cargar los candidatos.",
          retry: recargar,
        });
      })
      .finally(() => {
        if (controller.signal.aborted) return;
        setCargando(false);
        setCargandoMas(false);
      });

    return () => {
      controller.abort();
      siguientePeticion.current?.abort();
    };
  }, [consulta, recargar, reportError, clearError]);

  function aplicar(nuevos: FiltrosCandidatos) {
    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ filtros: normalizar(nuevos), revision: anterior.revision + 1 }));
  }

  function buscar() {
    if (filtros.nroCandidato.trim() && !/^[1-9][0-9]*$/.test(filtros.nroCandidato.trim())) {
      toast.error("Revisá el número de candidato", "Ingresá un número de candidato entero y positivo.");
      return;
    }

    aplicar(filtros);
  }

  function limpiar() {
    setFiltros(FILTROS_VACIOS);
    aplicar(FILTROS_VACIOS);
  }

  /** Quita un criterio ya aplicado: se borra del formulario y se vuelve a buscar sin él. */
  function quitarCriterio(criterio: keyof FiltrosCandidatos) {
    setFiltros((prev) => ({ ...prev, [criterio]: "" }));
    aplicar({ ...consulta.filtros, [criterio]: "" });
  }

  async function cargarMas() {
    if (cursor === null || cargando || siguientePeticion.current) return;

    const controller = new AbortController();
    siguientePeticion.current = controller;
    setCargandoMas(true);
    setError(false);

    try {
      const resultado = await listarCandidatos(consulta.filtros, cursor, controller.signal);
      if (controller.signal.aborted) return;

      setCandidatos((anteriores) => {
        const ids = new Set(anteriores.map((candidato) => candidato.id));
        return [...anteriores, ...resultado.candidatos.filter((candidato) => !ids.has(candidato.id))];
      });
      setCursor(resultado.siguienteCursor);
      clearError();
    } catch (error) {
      if (!controller.signal.aborted) {
        reportError(error, {
          title: "No pudimos cargar más candidatos",
          fallback: "No pudimos cargar la siguiente página.",
          retry: () => { void cargarMas(); },
        });
      }
    } finally {
      if (siguientePeticion.current === controller) siguientePeticion.current = null;
      if (!controller.signal.aborted) setCargandoMas(false);
    }
  }

  // Ante una caída del servicio, ocultamos controles y datos que dependen de él.
  // La navegación general pertenece al layout y sigue disponible.
  if (hasRetryError) return feedback;

  return (
    // layout={false}: cada búsqueda cambia la altura de la página, y la animación de layout
    // escalaba los contenedores y deformaba todo su contenido.
    <Stack gap="lg" className={styles.theme} layout={false}>
      <PageHeader
        eyebrow="Personas"
        title="Candidatos"
        description="Consultá los candidatos disponibles para evaluar o importá un archivo con altas, actualizaciones y bajas."
        actions={<ImportarCandidatosModal onImportado={recargar} />}
      />

      <CriteriosBusqueda
        columnas="minmax(0, 1fr) minmax(0, 1fr) minmax(0, 0.7fr)"
        campos={
          <>
            <TextField
              label="Apellido"
              name="apellido"
              value={filtros.apellido}
              onChange={(apellido) => setFiltros((prev) => ({ ...prev, apellido }))}
              maxLength={50}
              placeholder="Contiene…"
              autoComplete="off"
            />
            <TextField
              label="Nombre"
              name="nombre"
              value={filtros.nombre}
              onChange={(nombre) => setFiltros((prev) => ({ ...prev, nombre }))}
              maxLength={50}
              placeholder="Contiene…"
              autoComplete="off"
            />
            <TextField
              label="N.º de candidato"
              name="nroCandidato"
              value={filtros.nroCandidato}
              onChange={(nroCandidato) => setFiltros((prev) => ({ ...prev, nroCandidato }))}
              inputMode="numeric"
              maxLength={19}
              placeholder="Ej.: 101"
              autoComplete="off"
            />
          </>
        }
        onBuscar={buscar}
        onLimpiar={limpiar}
        puedeLimpiar={hayFiltrosEscritos || hayFiltros}
        aplicados={criteriosAplicados}
        sinCriterios="Mostrando todos los candidatos activos"
        resumen={
          cargando
            ? "Buscando…"
            : error
              ? ""
              : `${candidatos.length} ${candidatos.length === 1 ? "mostrado" : "mostrados"}`
        }
      />

      <Surface bordered padding="none" layout={false}>
        <div className="px-5 py-4">
          <h2 className="font-semibold">Candidatos activos</h2>
          <p className={styles.muted}>Los candidatos dados de baja no aparecen en este listado.</p>
        </div>

        {cargando && candidatos.length === 0 ? (
          <div className={styles.status} role="status">
            <Spinner size="sm" aria-hidden="true" /><span>Cargando candidatos…</span>
          </div>
        ) : candidatos.length ? (
          <div
            className={`${styles.tableScroll} mx-4 mb-4`}
            tabIndex={0}
            aria-label="Listado de candidatos activos"
            data-cargando={cargando}
            aria-busy={cargando}
          >
            <table className={styles.table}>
              <caption className="sr-only">Candidatos disponibles para una evaluación.</caption>
              <thead><tr>
                {["N.º", "Apellido y nombre", "Documento", "Email"].map((titulo) => (
                  <th key={titulo} scope="col">{titulo}</th>
                ))}
              </tr></thead>
              <tbody>
                {candidatos.map((candidato) => (
                  <tr key={candidato.id}>
                    <td className="tabular-nums">{candidato.nroCandidato}</td>
                    <td><span className={styles.candidateName}>{candidato.apellido}, {candidato.nombre}</span></td>
                    <td className="whitespace-nowrap">{candidato.tipoDocumento} {candidato.nroDocumento}</td>
                    <td className={styles.email}>{candidato.email}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : !error && (
          <div className={styles.empty}>
            <span className={styles.uploadIcon}><Icon name="users" width="26" height="26" /></span>
            <h3 className="text-lg font-semibold">{hayFiltros ? "Sin coincidencias" : "Todavía no hay candidatos"}</h3>
            <p className={`${styles.muted} max-w-lg`}>
              {hayFiltros
                ? "No se encontraron resultados para los criterios ingresados."
                : "Importá tu primer archivo CSV desde el botón Importar candidatos. Vas a poder revisar cada fila antes de confirmar."}
            </p>
          </div>
        )}

        {cursor !== null && !cargando && (
          <div className="flex justify-center border-t border-[var(--ui-border)] p-4">
            <Button variant="secondary" loading={cargandoMas} onClick={() => void cargarMas()}>
              {cargandoMas ? "Cargando…" : "Cargar más candidatos"}
            </Button>
          </div>
        )}
      </Surface>
    </Stack>
  );
}
