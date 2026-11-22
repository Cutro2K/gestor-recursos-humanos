"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Button, RadioGroup, Spinner, Stack, Surface, TextField } from "react-ui-lib";
import { CriteriosBusqueda } from "@/components/ui/criterios-busqueda";
import type { CriterioAplicado } from "@/components/ui/criterios-busqueda";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { listarCompetencias, obtenerTotalesCompetencias } from "@/service/competencias";
import type {
  Competencia,
  FiltrosCompetencias,
  TipoCompetencia,
  TotalesCompetencias,
} from "@/service/competencias";
import { AltaCompetenciaModal } from "./alta-competencia-modal";
import styles from "./competencias.module.css";

const FILTROS_INICIALES: FiltrosCompetencias = {
  nombre: "",
  codigo: "",
  tipo: "",
};

function tieneFiltros(filtros: FiltrosCompetencias): boolean {
  return Boolean(filtros.nombre?.trim() || filtros.codigo?.trim() || filtros.tipo);
}

function normalizar(filtros: FiltrosCompetencias): FiltrosCompetencias {
  return {
    nombre: filtros.nombre?.trim() ?? "",
    codigo: filtros.codigo?.trim() ?? "",
    tipo: filtros.tipo ?? "",
  };
}

const ETIQUETAS_TIPO: Record<TipoCompetencia, string> = {
  TECNICA: "Técnicas",
  ACTITUDINAL: "Actitudinales",
};

type CriterioTexto = "codigo" | "nombre";

const ETIQUETAS_CRITERIO: Record<CriterioTexto, string> = {
  codigo: "Código",
  nombre: "Nombre",
};

export function CompetenciasPanel() {
  // Lo que el consultor está escribiendo; se aplica recién con [Buscar], como en el resto
  // de las pantallas de gestión (ver CU008 y CU020 en el diseño de interfaces).
  const [borrador, setBorrador] = useState<FiltrosCompetencias>(FILTROS_INICIALES);
  const [consulta, setConsulta] = useState<{ filtros: FiltrosCompetencias; revision: number }>({
    filtros: FILTROS_INICIALES,
    revision: 0,
  });
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [cargando, setCargando] = useState(true);

  // Totales fijos del catálogo general (no afectados por filtros de búsqueda). null mientras
  // cargan o si fallaron: las tarjetas muestran "—" en vez de un 0 que no es real.
  const [totalesCatalogo, setTotalesCatalogo] = useState<TotalesCompetencias | null>(null);

  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargando,
  });

  const cargarTotales = useCallback((signal?: AbortSignal) => {
    return obtenerTotalesCompetencias(signal)
      .then((totales) => {
        if (signal?.aborted) return;
        setTotalesCatalogo(totales);
      })
      .catch(() => {
        if (signal?.aborted) return;
        // El error de red/servidor se reporta desde la consulta principal del panel.
        setTotalesCatalogo(null);
      });
  }, []);

  const recargar = useCallback(() => {
    setCargando(true);
    void cargarTotales();
    setConsulta((anterior) => ({ ...anterior, revision: anterior.revision + 1 }));
  }, [cargarTotales]);

  useEffect(() => {
    const controller = new AbortController();
    void cargarTotales(controller.signal);
    return () => controller.abort();
  }, [cargarTotales]);

  useEffect(() => {
    const controller = new AbortController();

    void listarCompetencias(consulta.filtros, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;
        setCompetencias(data);
        clearError();
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        setCompetencias([]);
        reportError(err, {
          title: "Error al consultar competencias",
          fallback: "No se pudieron obtener las competencias del servidor.",
          retry: recargar,
        });
      })
      .finally(() => {
        if (controller.signal.aborted) return;
        setCargando(false);
      });

    return () => controller.abort();
  }, [consulta, recargar, clearError, reportError]);

  function aplicar(filtros: FiltrosCompetencias) {
    setCargando(true);
    setConsulta((prev) => ({ filtros: normalizar(filtros), revision: prev.revision + 1 }));
  }

  function handleLimpiar() {
    setBorrador(FILTROS_INICIALES);
    aplicar(FILTROS_INICIALES);
  }

  /** Quita un criterio ya aplicado: se borra del formulario y se vuelve a buscar sin él. */
  function quitarCriterio(criterio: keyof FiltrosCompetencias) {
    setBorrador((prev) => ({ ...prev, [criterio]: "" }));
    aplicar({ ...consulta.filtros, [criterio]: "" });
  }

  // La búsqueda que realmente se ejecutó: decide las fichas y el mensaje sin resultados.
  const filtrosAplicados = consulta.filtros;
  const hayFiltrosAplicados = tieneFiltros(filtrosAplicados);
  const hayFiltrosEscritos = tieneFiltros(borrador);

  const criteriosAplicados: CriterioAplicado[] = [];
  for (const criterio of ["codigo", "nombre"] as const) {
    const valor = filtrosAplicados[criterio];
    if (valor) {
      criteriosAplicados.push({
        clave: criterio,
        etiqueta: ETIQUETAS_CRITERIO[criterio],
        valor: `“${valor}”`,
        onQuitar: () => quitarCriterio(criterio),
      });
    }
  }
  if (filtrosAplicados.tipo) {
    criteriosAplicados.push({
      clave: "tipo",
      etiqueta: "Tipo",
      valor: ETIQUETAS_TIPO[filtrosAplicados.tipo],
      onQuitar: () => quitarCriterio("tipo"),
    });
  }

  if (hasRetryError) {
    return feedback;
  }

  return (
    // layout={false}: cada búsqueda cambia la altura de la página, y la animación de layout
    // escalaba los contenedores y deformaba todo su contenido.
    <Stack gap="lg" className={styles.theme} layout={false}>
      <PageHeader
        eyebrow="Catálogos"
        title="Competencias"
        description="Gestioná las capacidades y habilidades profesionales que conforman los perfiles de puestos y estructuran las evaluaciones de candidatos (CU 13)."
        actions={<AltaCompetenciaModal onCreada={recargar} />}
      />

      <div className={styles.summary}>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Total Competencias</span>
          <span className={styles.summaryNumber}>{totalesCatalogo?.total ?? "—"}</span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Técnicas</span>
          <span className={`${styles.summaryNumber} ${styles.summaryTecnica}`}>
            {totalesCatalogo?.tecnicas ?? "—"}
          </span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Actitudinales</span>
          <span className={`${styles.summaryNumber} ${styles.summaryActitudinal}`}>
            {totalesCatalogo?.actitudinales ?? "—"}
          </span>
        </div>
      </div>

      <CriteriosBusqueda
        columnas="minmax(0, 1fr) minmax(0, 2fr)"
        campos={
          <>
            <TextField
              label="Código"
              name="codigo"
              uppercase
              value={borrador.codigo ?? ""}
              onChange={(codigo) => setBorrador((prev) => ({ ...prev, codigo }))}
              maxLength={20}
              placeholder="Ej.: COMP-01"
              autoComplete="off"
            />
            <TextField
              label="Nombre"
              name="nombre"
              value={borrador.nombre ?? ""}
              onChange={(nombre) => setBorrador((prev) => ({ ...prev, nombre }))}
              maxLength={75}
              placeholder="Contiene…"
              autoComplete="off"
            />
          </>
        }
        opciones={
          <RadioGroup
            label="Tipo"
            name="tipo"
            orientation="horizontal"
            value={borrador.tipo ?? ""}
            onValueChange={(tipo) =>
              setBorrador((prev) => ({ ...prev, tipo: tipo as TipoCompetencia | "" }))
            }
            options={[
              { value: "", label: "Todas" },
              { value: "TECNICA", label: ETIQUETAS_TIPO.TECNICA },
              { value: "ACTITUDINAL", label: ETIQUETAS_TIPO.ACTITUDINAL },
            ]}
          />
        }
        onBuscar={() => aplicar(borrador)}
        onLimpiar={handleLimpiar}
        puedeLimpiar={hayFiltrosEscritos || hayFiltrosAplicados}
        aplicados={criteriosAplicados}
        sinCriterios="Mostrando todo el catálogo"
        resumen={
          cargando
            ? "Buscando…"
            : `${competencias.length} ${competencias.length === 1 ? "resultado" : "resultados"}`
        }
      />

      {cargando && competencias.length === 0 ? (
        <Surface bordered style={{ minHeight: "220px", display: "grid", placeItems: "center" }}>
          <Stack align="center" gap="sm">
            <Spinner size="lg" />
            <p style={{ color: "var(--ui-muted)", fontSize: "0.875rem" }}>Cargando catálogo de competencias…</p>
          </Stack>
        </Surface>
      ) : competencias.length === 0 ? (
        <Surface bordered layout={false} className={styles.emptyState}>
          <div className={styles.emptyIcon}>
            <Icon name="target" width="28" height="28" />
          </div>
          <h3 style={{ fontSize: "1.1rem", fontWeight: 600 }}>No se encontraron competencias</h3>
          <p style={{ color: "var(--ui-muted)", maxWidth: "400px", fontSize: "0.875rem" }}>
            {hayFiltrosAplicados
              ? "No se encontraron resultados para los criterios ingresados."
              : "Todavía no se dieron de alta competencias en el sistema."}
          </p>
          {hayFiltrosAplicados ? (
            <Button variant="secondary" onClick={handleLimpiar}>
              Limpiar filtros
            </Button>
          ) : (
            <AltaCompetenciaModal onCreada={recargar} />
          )}
        </Surface>
      ) : (
        <div className={styles.tableScroll} data-cargando={cargando} aria-busy={cargando}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th style={{ width: "130px" }}>Código</th>
                <th>Nombre</th>
                <th style={{ width: "140px" }}>Tipo</th>
                <th>Descripción</th>
                <th style={{ width: "130px", textAlign: "right" }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {competencias.map((comp) => (
                <tr key={comp.id}>
                  <td>
                    <span className={styles.codeBadge}>{comp.codigo}</span>
                  </td>
                  <td>
                    <span className={styles.competenciaNombre}>{comp.nombre}</span>
                  </td>
                  <td>
                    <span
                      className={`${styles.badge} ${
                        comp.tipo === "TECNICA" ? styles.badgeTecnica : styles.badgeActitudinal
                      }`}
                    >
                      {comp.tipo === "TECNICA" ? "Técnica" : "Actitudinal"}
                    </span>
                  </td>
                  <td>
                    <div className={styles.competenciaDesc}>
                      {comp.descripcion || <span style={{ opacity: 0.5, fontStyle: "italic" }}>Sin descripción</span>}
                    </div>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <Link
                      href={`/factores?competenciaId=${comp.id}`}
                      style={{
                        color: "var(--ui-accent)",
                        fontSize: "0.825rem",
                        fontWeight: 600,
                        textDecoration: "none",
                        display: "inline-flex",
                        alignItems: "center",
                        gap: "4px",
                      }}
                    >
                      Ver factores <Icon name="arrow" width="14" height="14" />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Stack>
  );
}
