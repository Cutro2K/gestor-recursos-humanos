"use client";

import type { ReactNode } from "react";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import type { Variants } from "motion/react";
import { Button, Modal, Stack, Surface } from "react-ui-lib";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { consultarCompetenciasPuesto, listarPuestos } from "@/service/evaluaciones";
import type { CompetenciaPuesto, Puesto } from "@/service/evaluaciones";
import styles from "./evaluaciones.module.css";

const SUAVE = [0.25, 1, 0.5, 1] as const;

/**
 * La sección de competencias primero agranda la tarjeta hacia abajo y después muestra su
 * contenido; al ocultarse, el contenido se desvanece antes de que la tarjeta se achique.
 */
const seccionCompetencias: Variants = {
  oculta: { height: 0, transition: { duration: 0.25, ease: SUAVE, when: "afterChildren" } },
  visible: { height: "auto", transition: { duration: 0.3, ease: SUAVE, when: "beforeChildren" } },
};

const contenidoCompetencias: Variants = {
  oculta: { opacity: 0, y: -6, transition: { duration: 0.15 } },
  visible: { opacity: 1, y: 0, transition: { duration: 0.2, ease: SUAVE } },
};

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
  const reducirMovimiento = useReducedMotion();
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
        setCompetencias(null);
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

  // Al cambiar de puesto, la tabla anterior queda atenuada hasta que llegan las competencias nuevas:
  // así el contenedor solo cambia de altura una vez, en lugar de vaciarse y volver a crecer.
  function elegirPuesto(id: string) {
    setPuestoId(id);
    setErrorPuesto(null);
    if (!id) setCompetencias(null);
    setCargandoCompetencias(Boolean(id));
  }

  const puesto = puestos.find((p) => String(p.id) === puestoId) ?? null;
  // Mientras cargan, las competencias en pantalla son las del puesto anterior: no se evalúan.
  const vigentes = cargandoCompetencias ? null : competencias;
  const noEvaluables = vigentes?.filter((c) => !c.evaluable) ?? [];
  const sinCompetencias = vigentes !== null && vigentes.length === 0;

  /** Paso 10 y flujo 10.A. */
  function siguiente() {
    if (!puesto) {
      setErrorPuesto("Debe seleccionar una función o puesto.");
      return;
    }
    if (vigentes === null || sinCompetencias || noEvaluables.length > 0) return;
    onSiguiente(puesto);
  }

  if (hasRetryError) return feedback;

  return (
    <Stack gap="lg" layout={false}>
      {encabezado}
      {feedback}

      <Surface as="section" bordered padding="lg" layout={false} aria-labelledby="titulo-puesto">
        {/* layout={false}: la animación de layout escalaba el contenido cuando la tarjeta crecía. */}
        <Stack gap="lg" layout={false}>
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
        </Stack>

        {/* Fuera del Stack: así el espacio de separación también se anima en lugar de aparecer de golpe. */}
        <AnimatePresence initial={false}>
          {puestoId && competencias !== null && (
            <motion.div
              key="competencias"
              className="overflow-hidden"
              variants={seccionCompetencias}
              initial="oculta"
              animate="visible"
              exit="oculta"
              transition={reducirMovimiento ? { duration: 0 } : undefined}
            >
              <motion.div
                className="pt-6"
                variants={contenidoCompetencias}
                transition={reducirMovimiento ? { duration: 0 } : undefined}
              >
                <h3 className="mb-2 text-sm font-semibold">Competencias requeridas</h3>
                {competencias.length === 0 ? (
                  <p className={styles.vacio}>
                    Esta función o puesto no tiene competencias asociadas, así que no puede evaluarse.
                  </p>
                ) : (
                  <div
                    className={styles.tableScroll}
                    style={{ margin: 0 }}
                    tabIndex={0}
                    aria-label="Competencias requeridas"
                    data-cargando={cargandoCompetencias}
                    aria-busy={cargandoCompetencias}
                  >
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
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </Surface>

      <div className={styles.accionesPaso}>
        <Button variant="secondary" onClick={onVolver}>
          Volver
        </Button>
        <Button onClick={siguiente} disabled={sinCompetencias}>
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
    </Stack>
  );
}
