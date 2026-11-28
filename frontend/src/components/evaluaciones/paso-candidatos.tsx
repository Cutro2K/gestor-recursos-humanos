"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Modal, Spinner, Surface, TextField, useToast } from "react-ui-lib";
import { CriteriosBusqueda } from "@/components/ui/criterios-busqueda";
import type { CriterioAplicado } from "@/components/ui/criterios-busqueda";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { ApiError } from "@/service/api";
import { listarCandidatos } from "@/service/candidatos";
import type { Candidato, FiltrosCandidatos } from "@/service/candidatos";
import { verificarCandidatos } from "@/service/evaluaciones";
import styles from "./evaluaciones.module.css";

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

function alternar(conjunto: Set<number>, id: number) {
  const nuevo = new Set(conjunto);
  if (nuevo.has(id)) nuevo.delete(id);
  else nuevo.add(id);
  return nuevo;
}

type PasoCandidatosProps = {
  aEvaluar: Candidato[];
  onCambiarAEvaluar: (candidatos: Candidato[]) => void;
  onSiguiente: () => void;
};

/**
 * Pasos 2 a 6 del CU 25: búsqueda por Apellido, Nombre y N.º de candidato, [Agregar] a la
 * grilla "Candidatos a evaluar", [Quitar] y [Siguiente], que verifica que ninguno tenga
 * un cuestionario activo o en proceso (6.A).
 */
export function PasoCandidatos({ aEvaluar, onCambiarAEvaluar, onSiguiente }: PasoCandidatosProps) {
  const router = useRouter();
  const toast = useToast();
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [consulta, setConsulta] = useState({ filtros: FILTROS_VACIOS, revision: 0 });
  const [resultados, setResultados] = useState<Candidato[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [cargandoMas, setCargandoMas] = useState(false);
  const [seleccionResultados, setSeleccionResultados] = useState<Set<number>>(new Set());
  const [seleccionAEvaluar, setSeleccionAEvaluar] = useState<Set<number>>(new Set());
  const [verificando, setVerificando] = useState(false);
  const [avisoCuestionarios, setAvisoCuestionarios] = useState<string | null>(null);
  const siguientePagina = useRef<AbortController | null>(null);
  const { reportError, feedback, clearError } = useErrorFeedback({ retrying: cargando || cargandoMas });

  const idsAEvaluar = useMemo(() => new Set(aEvaluar.map((c) => c.id)), [aEvaluar]);
  const hayFiltros = Object.values(consulta.filtros).some(Boolean);
  const hayFiltrosEscritos = Object.values(filtros).some((valor) => valor.trim());

  const recargar = useCallback(() => {
    setCargando(true);
    setConsulta((anterior) => ({ ...anterior, revision: anterior.revision + 1 }));
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    void listarCandidatos(consulta.filtros, null, controller.signal)
      .then((resultado) => {
        if (controller.signal.aborted) return;
        setResultados(resultado.candidatos);
        setCursor(resultado.siguienteCursor);
        setSeleccionResultados(new Set());
        clearError();
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setResultados([]);
        setCursor(null);
        reportError(error, {
          title: "No pudimos completar la búsqueda",
          fallback: "No pudimos cargar los candidatos.",
          retry: recargar,
        });
      })
      .finally(() => {
        if (!controller.signal.aborted) setCargando(false);
      });

    return () => {
      controller.abort();
      siguientePagina.current?.abort();
    };
  }, [consulta, recargar, reportError, clearError]);

  function aplicar(nuevos: FiltrosCandidatos) {
    setCargando(true);
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

  function quitarCriterio(criterio: keyof FiltrosCandidatos) {
    setFiltros((prev) => ({ ...prev, [criterio]: "" }));
    aplicar({ ...consulta.filtros, [criterio]: "" });
  }

  const criteriosAplicados: CriterioAplicado[] = (["apellido", "nombre", "nroCandidato"] as const)
    .filter((criterio) => consulta.filtros[criterio])
    .map((criterio) => ({
      clave: criterio,
      etiqueta: ETIQUETAS_CRITERIO[criterio],
      valor: criterio === "nroCandidato" ? consulta.filtros[criterio] : `“${consulta.filtros[criterio]}”`,
      onQuitar: () => quitarCriterio(criterio),
    }));

  async function cargarMas() {
    if (cursor === null || cargando || siguientePagina.current) return;
    const controller = new AbortController();
    siguientePagina.current = controller;
    setCargandoMas(true);
    try {
      const resultado = await listarCandidatos(consulta.filtros, cursor, controller.signal);
      if (controller.signal.aborted) return;
      setResultados((anteriores) => {
        const ids = new Set(anteriores.map((c) => c.id));
        return [...anteriores, ...resultado.candidatos.filter((c) => !ids.has(c.id))];
      });
      setCursor(resultado.siguienteCursor);
    } catch (error) {
      if (!controller.signal.aborted) {
        reportError(error, {
          title: "No pudimos cargar más candidatos",
          fallback: "No pudimos cargar la siguiente página.",
        });
      }
    } finally {
      if (siguientePagina.current === controller) siguientePagina.current = null;
      if (!controller.signal.aborted) setCargandoMas(false);
    }
  }

  /** Paso 4 y flujo 4.A. */
  function agregar() {
    const nuevos = resultados.filter((c) => seleccionResultados.has(c.id) && !idsAEvaluar.has(c.id));
    if (nuevos.length === 0) {
      toast.error("No hay candidatos seleccionados", "Debe seleccionar uno o más candidatos.");
      return;
    }
    onCambiarAEvaluar([...aEvaluar, ...nuevos]);
    setSeleccionResultados(new Set());
  }

  /** Flujo 5.A. */
  function quitar() {
    if (seleccionAEvaluar.size === 0) {
      toast.error(
        "No hay candidatos seleccionados",
        "Seleccioná uno o más candidatos de la grilla Candidatos a evaluar para quitarlos.",
      );
      return;
    }
    onCambiarAEvaluar(aEvaluar.filter((c) => !seleccionAEvaluar.has(c.id)));
    setSeleccionAEvaluar(new Set());
  }

  /** Paso 6 y flujo 6.A. */
  async function siguiente() {
    if (aEvaluar.length === 0) {
      toast.error(
        "No hay candidatos para evaluar",
        "Debe seleccionar al menos un candidato de la grilla para continuar.",
      );
      return;
    }
    setVerificando(true);
    try {
      await verificarCandidatos(aEvaluar.map((c) => c.id));
      onSiguiente();
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setAvisoCuestionarios(error.message);
      } else {
        reportError(error, {
          title: "No pudimos verificar a los candidatos",
          fallback: "No pudimos verificar si los candidatos tienen cuestionarios activos.",
        });
      }
    } finally {
      setVerificando(false);
    }
  }

  const resultadosSeleccionables = resultados.filter((c) => !idsAEvaluar.has(c.id));
  const todosResultadosSeleccionados =
    resultadosSeleccionables.length > 0 && resultadosSeleccionables.every((c) => seleccionResultados.has(c.id));

  return (
    <>
      {feedback}

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
              maxLength={10}
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
        resumen={cargando ? "Buscando…" : `${resultados.length} ${resultados.length === 1 ? "mostrado" : "mostrados"}`}
      />

      <Surface as="section" bordered padding="none" layout={false} aria-labelledby="titulo-resultados">
        <div className={styles.encabezadoGrilla}>
          <h2 id="titulo-resultados" className="font-semibold">Resultados de la búsqueda</h2>
          <span className={styles.muted}>{seleccionResultados.size} seleccionados</span>
        </div>

        {cargando && resultados.length === 0 ? (
          <div className={styles.estado} role="status">
            <Spinner size="sm" aria-hidden="true" />
            <span>Buscando candidatos…</span>
          </div>
        ) : resultados.length === 0 ? (
          <p className={styles.vacio}>
            {hayFiltros
              ? "No se encontraron candidatos para los criterios ingresados."
              : "Todavía no hay candidatos activos."}
          </p>
        ) : (
          <div
            className={styles.tableScroll}
            tabIndex={0}
            aria-label="Resultados de la búsqueda"
            data-cargando={cargando}
            aria-busy={cargando}
          >
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col" className={styles.colSel}>
                    <input
                      type="checkbox"
                      className={styles.checkbox}
                      aria-label="Seleccionar todos los resultados"
                      checked={todosResultadosSeleccionados}
                      disabled={resultadosSeleccionables.length === 0}
                      onChange={() =>
                        setSeleccionResultados(
                          todosResultadosSeleccionados
                            ? new Set()
                            : new Set(resultadosSeleccionables.map((c) => c.id)),
                        )
                      }
                    />
                  </th>
                  <th scope="col">Apellido</th>
                  <th scope="col">Nombre</th>
                  <th scope="col">N.º de candidato</th>
                </tr>
              </thead>
              <tbody>
                {resultados.map((candidato) => {
                  const agregado = idsAEvaluar.has(candidato.id);
                  const seleccionado = seleccionResultados.has(candidato.id);
                  return (
                    <tr key={candidato.id} data-seleccionada={seleccionado}>
                      <td>
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          aria-label={`Seleccionar a ${candidato.apellido}, ${candidato.nombre}`}
                          checked={agregado || seleccionado}
                          disabled={agregado}
                          onChange={() => setSeleccionResultados((actual) => alternar(actual, candidato.id))}
                        />
                      </td>
                      <td>{candidato.apellido}</td>
                      <td>
                        {candidato.nombre}
                        {agregado && <span className={`${styles.muted} ml-2`}>(ya agregado)</span>}
                      </td>
                      <td className="tabular-nums">{candidato.nroCandidato}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className={styles.accionesGrilla}>
          {cursor !== null && !cargando && (
            <Button variant="secondary" loading={cargandoMas} onClick={() => void cargarMas()}>
              {cargandoMas ? "Cargando…" : "Cargar más"}
            </Button>
          )}
          <Button onClick={agregar} disabled={cargando}>
            Agregar
          </Button>
        </div>
      </Surface>

      <Surface as="section" bordered padding="none" layout={false} aria-labelledby="titulo-a-evaluar">
        <div className={styles.encabezadoGrilla}>
          <h2 id="titulo-a-evaluar" className="font-semibold">Candidatos a evaluar</h2>
          <span className={styles.muted}>
            {aEvaluar.length} {aEvaluar.length === 1 ? "candidato" : "candidatos"}
          </span>
        </div>

        {aEvaluar.length === 0 ? (
          <p className={styles.vacio}>
            Seleccioná candidatos en los resultados de la búsqueda y presioná Agregar.
          </p>
        ) : (
          <div className={styles.tableScroll} tabIndex={0} aria-label="Candidatos a evaluar">
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col" className={styles.colSel}>
                    <span className="sr-only">Seleccionar</span>
                  </th>
                  <th scope="col">Apellido</th>
                  <th scope="col">Nombre</th>
                  <th scope="col">N.º de candidato</th>
                </tr>
              </thead>
              <tbody>
                {aEvaluar.map((candidato) => {
                  const seleccionado = seleccionAEvaluar.has(candidato.id);
                  return (
                    <tr key={candidato.id} data-seleccionada={seleccionado}>
                      <td>
                        <input
                          type="checkbox"
                          className={styles.checkbox}
                          aria-label={`Seleccionar a ${candidato.apellido}, ${candidato.nombre} para quitar`}
                          checked={seleccionado}
                          onChange={() => setSeleccionAEvaluar((actual) => alternar(actual, candidato.id))}
                        />
                      </td>
                      <td>{candidato.apellido}</td>
                      <td>{candidato.nombre}</td>
                      <td className="tabular-nums">{candidato.nroCandidato}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <div className={styles.accionesGrilla}>
          <Button variant="secondary" onClick={quitar} disabled={aEvaluar.length === 0}>
            Quitar
          </Button>
        </div>
      </Surface>

      <div className={styles.accionesPaso}>
        <Button variant="secondary" onClick={() => router.push("/")} disabled={verificando}>
          Salir
        </Button>
        <Button onClick={() => void siguiente()} loading={verificando} disabled={verificando}>
          {verificando ? "Verificando…" : "Siguiente"}
        </Button>
      </div>

      <Modal
        open={avisoCuestionarios !== null}
        onOpenChange={(abierto) => {
          if (!abierto) setAvisoCuestionarios(null);
        }}
        title="Cuestionarios activos existentes"
        description={avisoCuestionarios ?? undefined}
        actions={<Button onClick={() => setAvisoCuestionarios(null)}>Aceptar</Button>}
      >
        <p className={styles.muted}>
          Quitá a esos candidatos de la grilla Candidatos a evaluar para continuar.
        </p>
      </Modal>
    </>
  );
}
