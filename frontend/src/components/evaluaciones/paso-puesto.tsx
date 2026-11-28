"use client";

import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Modal, Spinner, Stack, Surface } from "react-ui-lib";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { consultarCompetenciasPuesto, listarPuestos } from "@/service/evaluaciones";
import type { CompetenciaPuesto, Puesto } from "@/service/evaluaciones";
import styles from "./evaluaciones.module.css";

const formatoPonderacion = new Intl.NumberFormat("es-AR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

type PasoPuestoProps = {
  encabezado: ReactNode;
  puestoInicial: Puesto | null;
  onVolver: () => void;
  onSiguiente: (puesto: Puesto) => void;
};

/**
 * Pasos 7 a 10 del CU 25: el actor elige la función o puesto (nombre y empresa) y el sistema
 * muestra sus competencias con la ponderación requerida. Si alguna no puede evaluarse (9.A),
 * informa cuáles y el caso de uso finaliza.
 */
export function PasoPuesto({ encabezado, puestoInicial, onVolver, onSiguiente }: PasoPuestoProps) {
  const router = useRouter();
  const [puestos, setPuestos] = useState<Puesto[]>([]);
  const [cargandoPuestos, setCargandoPuestos] = useState(true);
  const [puestoId, setPuestoId] = useState(puestoInicial ? String(puestoInicial.id) : "");
  const [competencias, setCompetencias] = useState<CompetenciaPuesto[] | null>(null);
  const [cargandoCompetencias, setCargandoCompetencias] = useState(puestoInicial !== null);
  const [errorPuesto, setErrorPuesto] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargandoPuestos || cargandoCompetencias,
  });

  useEffect(() => {
    const controller = new AbortController();
    void listarPuestos(controller.signal)
      .then((lista) => {
        if (controller.signal.aborted) return;
        setPuestos(lista);
        clearError();
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        reportError(error, {
          title: "No pudimos cargar los puestos",
          fallback: "No pudimos cargar las funciones o puestos.",
          retry: () => {
            setCargandoPuestos(true);
            setRevision((r) => r + 1);
          },
        });
      })
      .finally(() => {
        if (!controller.signal.aborted) setCargandoPuestos(false);
      });
    return () => controller.abort();
  }, [revision, reportError, clearError]);

  useEffect(() => {
    if (!puestoId) return;
    const controller = new AbortController();
    void consultarCompetenciasPuesto(Number(puestoId), controller.signal)
      .then((lista) => {
        if (!controller.signal.aborted) setCompetencias(lista);
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        reportError(error, {
          title: "No pudimos cargar las competencias",
          fallback: "No pudimos cargar las competencias del puesto.",
        });
      })
      .finally(() => {
        if (!controller.signal.aborted) setCargandoCompetencias(false);
      });
    return () => controller.abort();
  }, [puestoId, reportError]);

  function elegirPuesto(id: string) {
    setPuestoId(id);
    setErrorPuesto(null);
    setCompetencias(null);
    setCargandoCompetencias(Boolean(id));
  }

  const puesto = puestos.find((p) => String(p.id) === puestoId) ?? null;
  const noEvaluables = competencias?.filter((c) => !c.evaluable) ?? [];
  const sinCompetencias = competencias !== null && competencias.length === 0;

  /** Paso 10 y flujo 10.A. */
  function siguiente() {
    if (!puesto) {
      setErrorPuesto("Debe seleccionar una función o puesto.");
      return;
    }
    if (competencias === null || sinCompetencias || noEvaluables.length > 0) return;
    onSiguiente(puesto);
  }

  if (hasRetryError) return feedback;

  return (
    <>
      {encabezado}
      {feedback}

      <Surface as="section" bordered padding="lg" layout={false} aria-labelledby="titulo-puesto">
        <Stack gap="lg">
          <h2 id="titulo-puesto" className="font-semibold">Función o puesto a evaluar</h2>

          <div className={styles.campo}>
            <label htmlFor="evaluacion-puesto" className={styles.etiqueta}>
              Función o puesto *
            </label>
            <select
              id="evaluacion-puesto"
              name="puestoId"
              className={styles.selectControl}
              value={puestoId}
              disabled={cargandoPuestos}
              aria-invalid={Boolean(errorPuesto)}
              aria-describedby={errorPuesto ? "evaluacion-puesto-error" : undefined}
              onChange={(e) => elegirPuesto(e.target.value)}
            >
              <option value="">
                {cargandoPuestos
                  ? "Cargando funciones o puestos…"
                  : puestos.length === 0
                    ? "No hay funciones o puestos activos"
                    : "Seleccionar función o puesto…"}
              </option>
              {puestos.map((p) => (
                <option key={p.id} value={String(p.id)}>
                  {p.nombre} — {p.empresa}
                </option>
              ))}
            </select>
            {errorPuesto && (
              <span id="evaluacion-puesto-error" className={styles.error} role="alert">
                {errorPuesto}
              </span>
            )}
          </div>

          {puestoId && (
            <div>
              <h3 className="mb-2 text-sm font-semibold">Competencias requeridas</h3>
              {cargandoCompetencias || competencias === null ? (
                <div className={styles.estado} role="status">
                  <Spinner size="sm" aria-hidden="true" />
                  <span>Cargando competencias…</span>
                </div>
              ) : sinCompetencias ? (
                <p className={styles.vacio}>
                  Esta función o puesto no tiene competencias asociadas, así que no puede evaluarse.
                </p>
              ) : (
                <div className={styles.tableScroll} style={{ margin: 0 }} tabIndex={0} aria-label="Competencias requeridas">
                  <table className={styles.table}>
                    <thead>
                      <tr>
                        <th scope="col">Competencia</th>
                        <th scope="col">Ponderación requerida</th>
                      </tr>
                    </thead>
                    <tbody>
                      {competencias.map((competencia) => (
                        <tr key={competencia.competenciaId}>
                          <td>{competencia.nombre}</td>
                          <td className="tabular-nums">{formatoPonderacion.format(competencia.ponderacionMinima)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </Stack>
      </Surface>

      <div className={styles.accionesPaso}>
        <Button variant="secondary" onClick={onVolver}>
          Volver
        </Button>
        <Button onClick={siguiente} disabled={cargandoCompetencias || sinCompetencias}>
          Siguiente
        </Button>
      </div>

      <Modal
        open={noEvaluables.length > 0}
        onOpenChange={() => router.push("/")}
        closeOnEscape={false}
        closeOnOutsideClick={false}
        showCloseButton={false}
        title="Competencias que no pueden ser evaluadas"
        description={`La función o puesto ${puesto?.nombre ?? ""} tiene competencias sin ningún factor con al menos 2 preguntas activas, así que los candidatos no pueden ser evaluados.`}
        actions={<Button onClick={() => router.push("/")}>Aceptar</Button>}
      >
        <ul className={styles.listaNombres}>
          {noEvaluables.map((c) => (
            <li key={c.competenciaId}>{c.nombre}</li>
          ))}
        </ul>
      </Modal>
    </>
  );
}
