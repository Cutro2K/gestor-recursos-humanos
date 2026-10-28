"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Button, Spinner, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { Icon } from "@/components/ui/icon";
import { PageHeader } from "@/components/ui/page-header";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { listarCandidatos } from "@/service/candidatos";
import type { Candidato, FiltrosCandidatos } from "@/service/candidatos";
import { ImportarCandidatosModal } from "./importar-candidatos-modal";
import styles from "./candidatos.module.css";

const FILTROS_VACIOS: FiltrosCandidatos = { apellido: "", nombre: "", nroCandidato: "" };

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

  function buscar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (filtros.nroCandidato.trim() && !/^[1-9][0-9]*$/.test(filtros.nroCandidato.trim())) {
      toast.error("Revisá el número de candidato", "Ingresá un número de candidato entero y positivo.");
      return;
    }

    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ filtros, revision: anterior.revision + 1 }));
  }

  function limpiar() {
    setFiltros(FILTROS_VACIOS);
    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ filtros: FILTROS_VACIOS, revision: anterior.revision + 1 }));
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
    <Stack gap="lg" className={styles.theme}>
      <PageHeader
        eyebrow="Personas"
        title="Candidatos"
        description="Consultá los candidatos disponibles para evaluar o importá un archivo con altas, actualizaciones y bajas."
        actions={<ImportarCandidatosModal onImportado={recargar} />}
      />

      <Surface bordered>
        <form className={styles.filters} onSubmit={buscar} noValidate aria-label="Buscar candidatos">
          <TextField
            label="Apellido"
            value={filtros.apellido}
            onChange={(apellido) => setFiltros({ ...filtros, apellido })}
            maxLength={50}
            placeholder="Buscar por apellido"
          />
          <TextField
            label="Nombre"
            value={filtros.nombre}
            onChange={(nombre) => setFiltros({ ...filtros, nombre })}
            maxLength={50}
            placeholder="Buscar por nombre"
          />
          <TextField
            label="N.º de candidato"
            value={filtros.nroCandidato}
            onChange={(nroCandidato) => setFiltros({ ...filtros, nroCandidato })}
            inputMode="numeric"
            maxLength={19}
            placeholder="Ej. 101"
          />
          <Stack direction="row" gap="sm" wrap>
            <Button type="submit" disabled={cargando}>Buscar</Button>
            <Button type="button" variant="secondary" disabled={cargando} onClick={limpiar}>Limpiar</Button>
          </Stack>
        </form>
      </Surface>

      <Surface bordered padding="none">
        <Stack direction="row" justify="between" align="center" gap="md" wrap className="px-5 py-4">
          <div>
            <h2 className="font-semibold">Candidatos activos</h2>
            <p className={styles.muted}>Los candidatos dados de baja no aparecen en este listado.</p>
          </div>
          {!cargando && !error && (
            <span className={styles.muted} aria-live="polite">{candidatos.length} mostrados</span>
          )}
        </Stack>

        {cargando ? (
          <div className={styles.status} role="status">
            <Spinner size="sm" aria-hidden="true" /><span>Cargando candidatos…</span>
          </div>
        ) : candidatos.length ? (
          <div className={`${styles.tableScroll} mx-4 mb-4`} tabIndex={0} aria-label="Listado de candidatos activos">
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
                ? "Probá con otros datos o limpiá los filtros para ver todos los candidatos activos."
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
