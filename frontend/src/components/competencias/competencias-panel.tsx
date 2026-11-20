"use client";

import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import Link from "next/link";
import { Button, Spinner, Stack, Surface, TextField } from "react-ui-lib";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { listarCompetencias } from "@/service/competencias";
import type { Competencia, FiltrosCompetencias, TipoCompetencia } from "@/service/competencias";
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

function SearchIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="m21 21-4.35-4.35M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z" />
    </svg>
  );
}

export function CompetenciasPanel() {
  const [filtros, setFiltros] = useState<FiltrosCompetencias>(FILTROS_INICIALES);
  const [consulta, setConsulta] = useState<{ filtros: FiltrosCompetencias; revision: number }>({
    filtros: FILTROS_INICIALES,
    revision: 0,
  });
  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [cargando, setCargando] = useState(true);

  // Totales fijos del catálogo general (no afectados por filtros de búsqueda)
  const [totalesCatalogo, setTotalesCatalogo] = useState<{
    total: number;
    tecnicas: number;
    actitudinales: number;
  }>({ total: 0, tecnicas: 0, actitudinales: 0 });
  const [cargandoTotales, setCargandoTotales] = useState(true);

  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargando,
  });

  const cargarTotales = useCallback((signal?: AbortSignal) => {
    return listarCompetencias({}, signal)
      .then((todas) => {
        if (signal?.aborted) return;
        setTotalesCatalogo({
          total: todas.length,
          tecnicas: todas.filter((c) => c.tipo === "TECNICA").length,
          actitudinales: todas.filter((c) => c.tipo === "ACTITUDINAL").length,
        });
      })
      .catch(() => {
        // El error de red/servidor se reporta desde la consulta principal del panel.
      })
      .finally(() => {
        if (signal?.aborted) return;
        setCargandoTotales(false);
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

  function handleBuscar(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setCargando(true);
    setConsulta((prev) => ({ filtros, revision: prev.revision + 1 }));
  }

  function handleLimpiar() {
    setFiltros(FILTROS_INICIALES);
    setCargando(true);
    setConsulta({ filtros: FILTROS_INICIALES, revision: consulta.revision + 1 });
  }

  function handleCambiarTipo(tipo: TipoCompetencia | "") {
    const nuevosFiltros: FiltrosCompetencias = { ...filtros, tipo };
    setFiltros(nuevosFiltros);
    setCargando(true);
    setConsulta((prev) => ({ filtros: nuevosFiltros, revision: prev.revision + 1 }));
  }

  // Lo escrito en el formulario: decide si se muestra "Limpiar" para borrarlo.
  const hayFiltrosEscritos = tieneFiltros(filtros);
  // La búsqueda que realmente se ejecutó: decide el mensaje cuando no hay resultados.
  const hayFiltrosAplicados = tieneFiltros(consulta.filtros);

  if (hasRetryError) {
    return feedback;
  }

  return (
    <Stack gap="lg" className={styles.theme}>
      <PageHeader
        eyebrow="Catálogos"
        title="Competencias"
        description="Gestioná las capacidades y habilidades profesionales que conforman los perfiles de puestos y estructuran las evaluaciones de candidatos (CU 13)."
        actions={<AltaCompetenciaModal onCreada={recargar} />}
      />

      <div className={styles.summary}>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Total Competencias</span>
          <span className={styles.summaryNumber}>{cargandoTotales ? "—" : totalesCatalogo.total}</span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Técnicas</span>
          <span className={`${styles.summaryNumber} ${styles.summaryTecnica}`}>
            {cargandoTotales ? "—" : totalesCatalogo.tecnicas}
          </span>
        </div>
        <div className={styles.summaryCard}>
          <span className={styles.summaryLabel}>Actitudinales</span>
          <span className={`${styles.summaryNumber} ${styles.summaryActitudinal}`}>
            {cargandoTotales ? "—" : totalesCatalogo.actitudinales}
          </span>
        </div>
      </div>

      <Surface bordered>
        <form className={styles.filters} onSubmit={handleBuscar} noValidate aria-label="Filtrar competencias">
          <div className={styles.filterField}>
            <TextField
              label="Buscar por nombre"
              placeholder="Ej: Liderazgo, Gestión..."
              value={filtros.nombre ?? ""}
              onChange={(nombre) => setFiltros({ ...filtros, nombre })}
            />
          </div>

          <div className={styles.filterField}>
            <TextField
              label="Buscar por código"
              placeholder="Ej: COMP-01"
              value={filtros.codigo ?? ""}
              onChange={(codigo) => setFiltros({ ...filtros, codigo })}
            />
          </div>

          <div className={styles.typeFilterGroup}>
            <button
              type="button"
              className={styles.typePill}
              data-active={!filtros.tipo}
              onClick={() => handleCambiarTipo("")}
            >
              Todas
            </button>
            <button
              type="button"
              className={styles.typePill}
              data-active={filtros.tipo === "TECNICA"}
              onClick={() => handleCambiarTipo("TECNICA")}
            >
              Técnicas
            </button>
            <button
              type="button"
              className={styles.typePill}
              data-active={filtros.tipo === "ACTITUDINAL"}
              onClick={() => handleCambiarTipo("ACTITUDINAL")}
            >
              Actitudinales
            </button>
          </div>

          <div className={styles.filterActions}>
            <Button type="submit" variant="secondary" leadingIcon={<SearchIcon />}>
              Buscar
            </Button>
            {hayFiltrosEscritos && (
              <Button type="button" variant="secondary" onClick={handleLimpiar}>
                Limpiar
              </Button>
            )}
          </div>
        </form>
      </Surface>

      {cargando ? (
        <Surface bordered style={{ minHeight: "220px", display: "grid", placeItems: "center" }}>
          <Stack align="center" gap="sm">
            <Spinner size="lg" />
            <p style={{ color: "var(--ui-muted)", fontSize: "0.875rem" }}>Cargando catálogo de competencias…</p>
          </Stack>
        </Surface>
      ) : competencias.length === 0 ? (
        <Surface bordered className={styles.emptyState}>
          <div className={styles.emptyIcon}>
            <Icon name="target" width="28" height="28" />
          </div>
          <h3 style={{ fontSize: "1.1rem", fontWeight: 600 }}>No se encontraron competencias</h3>
          <p style={{ color: "var(--ui-muted)", maxWidth: "400px", fontSize: "0.875rem" }}>
            {hayFiltrosAplicados
              ? "No hay competencias activas que coincidan con los criterios de búsqueda aplicados."
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
        <div className={styles.tableScroll}>
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
