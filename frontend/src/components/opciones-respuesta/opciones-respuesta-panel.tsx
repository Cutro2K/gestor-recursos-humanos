"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import type { Variants } from "motion/react";
import { Button, RadioButton, Spinner, Stack, Surface, TextField } from "react-ui-lib";
import { CriteriosBusqueda } from "../ui/criterios-busqueda";
import type { CriterioAplicado } from "../ui/criterios-busqueda";
import { PageHeader } from "../ui/page-header";
import { Icon } from "../ui/icon";
import { useErrorFeedback } from "../ui/use-error-feedback";
import { AccionEnDesarrolloModal } from "../ui/accion-en-desarrollo-modal";
import { AltaOpcionRespuestaModal } from "./alta-opcion-respuesta-modal";
import { listarOpcionesRespuesta } from "@/service/opciones-respuesta";
import type {
  FiltrosOpcionesRespuesta,
  ListadoOpcionRespuestaDTO,
} from "@/service/opciones-respuesta";
import styles from "./opciones-respuesta.module.css";

const FILTROS_VACIOS: FiltrosOpcionesRespuesta = { nombre: "" };

function normalizar(filtros: FiltrosOpcionesRespuesta): FiltrosOpcionesRespuesta {
  return {
    nombre: filtros.nombre.trim(),
  };
}

const EASE = [0.4, 0, 0.2, 1] as const;

const barraVariants: Variants = {
  oculta: { height: 0, transition: { duration: 0.2, ease: EASE, when: "afterChildren" } },
  visible: {
    height: "auto",
    transition: { duration: 0.22, ease: EASE, when: "beforeChildren", staggerChildren: 0.06 },
  },
};

const accionesVariants: Variants = {
  oculta: { opacity: 0, y: 6, transition: { duration: 0.12, ease: EASE } },
  visible: { opacity: 1, y: 0, transition: { duration: 0.18, ease: EASE } },
};

function sinAnimacion(variants: Variants): Variants {
  return Object.fromEntries(
    Object.entries(variants).map(([nombre, variante]) => [
      nombre,
      { ...variante, transition: { duration: 0 } },
    ]),
  ) as Variants;
}

const barraVariantsReducidas = sinAnimacion(barraVariants);
const accionesVariantsReducidas = sinAnimacion(accionesVariants);

export function OpcionesRespuestaPanel() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [consulta, setConsulta] = useState({ filtros: FILTROS_VACIOS, revision: 0 });
  const [opciones, setOpciones] = useState<ListadoOpcionRespuestaDTO[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [cursor, setCursor] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [cargandoMas, setCargandoMas] = useState(false);
  const [error, setError] = useState(false);
  const siguientePeticion = useRef<AbortController | null>(null);
  const reducirMovimiento = useReducedMotion();

  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargando || cargandoMas,
  });

  const hayFiltros = Boolean(consulta.filtros.nombre.trim());
  const hayFiltrosEscritos = Boolean(filtros.nombre.trim());

  const criteriosAplicados: CriterioAplicado[] = consulta.filtros.nombre
    ? [
        {
          clave: "nombre",
          etiqueta: "Nombre",
          valor: `“${consulta.filtros.nombre}”`,
          onQuitar: () => quitarCriterio(),
        },
      ]
    : [];

  const opcionSeleccionada =
    selectedId !== null ? (opciones.find((o) => o.id === selectedId) ?? null) : null;

  const recargar = useCallback(() => {
    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ ...anterior, revision: anterior.revision + 1 }));
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    void listarOpcionesRespuesta(consulta.filtros, null, controller.signal)
      .then((resultado) => {
        if (controller.signal.aborted) return;
        setOpciones(resultado.opciones);
        setCursor(resultado.siguienteCursor);
        const idsRecibidos = new Set(resultado.opciones.map((o) => o.id));
        setSelectedId((prev) => (prev !== null && idsRecibidos.has(prev) ? prev : null));
        clearError();
      })
      .catch((err) => {
        if (controller.signal.aborted) return;
        setOpciones([]);
        setSelectedId(null);
        setCursor(null);
        setError(true);
        reportError(err, {
          title: "No pudimos completar la consulta",
          fallback: "No pudimos cargar las opciones de respuesta.",
          retry: recargar,
        });
      })
      .finally(() => {
        if (controller.signal.aborted) return;
        setCargando(false);
      });

    return () => {
      controller.abort();
      siguientePeticion.current?.abort();
    };
  }, [consulta, recargar, reportError, clearError]);

  function alternarSeleccion(id: number | null | undefined) {
    if (typeof id !== "number") return;
    setSelectedId((prev) => (prev === id ? null : id));
  }

  function aplicar(nuevos: FiltrosOpcionesRespuesta) {
    setCargando(true);
    setError(false);
    setSelectedId(null);
    setConsulta((anterior) => ({ filtros: normalizar(nuevos), revision: anterior.revision + 1 }));
  }

  function limpiar() {
    setFiltros(FILTROS_VACIOS);
    aplicar(FILTROS_VACIOS);
  }

  function quitarCriterio() {
    setFiltros(FILTROS_VACIOS);
    aplicar(FILTROS_VACIOS);
  }

  async function cargarMas() {
    if (cursor === null || cargando || siguientePeticion.current) return;

    const controller = new AbortController();
    siguientePeticion.current = controller;
    setCargandoMas(true);
    setError(false);

    try {
      const resultado = await listarOpcionesRespuesta(consulta.filtros, cursor, controller.signal);
      if (controller.signal.aborted) return;
      setOpciones((anteriores) => [...anteriores, ...resultado.opciones]);
      setCursor(resultado.siguienteCursor);
    } catch (err) {
      if (controller.signal.aborted) return;
      reportError(err, {
        title: "No pudimos cargar más opciones de respuesta",
        fallback: "Ocurrió un error al cargar las siguientes opciones de respuesta.",
      });
    } finally {
      if (siguientePeticion.current === controller) siguientePeticion.current = null;
      if (!controller.signal.aborted) setCargandoMas(false);
    }
  }

  if (hasRetryError) return feedback;

  return (
    <Stack gap="lg" className={styles.theme} layout={false}>
      <PageHeader
        eyebrow="Catálogos"
        title="Opciones de respuesta"
        description="Consultá y administrá las opciones de respuesta reutilizables que estructuran las posibles respuestas de las preguntas."
        actions={<AltaOpcionRespuestaModal onCreada={recargar} />}
      />

      <CriteriosBusqueda
        campos={
          <TextField
            label="Nombre"
            name="nombre"
            value={filtros.nombre}
            onChange={(nombre) => setFiltros({ nombre })}
            maxLength={50}
            placeholder="Contiene…"
            autoComplete="off"
          />
        }
        onBuscar={() => aplicar(filtros)}
        onLimpiar={limpiar}
        puedeLimpiar={hayFiltrosEscritos || hayFiltros}
        aplicados={criteriosAplicados}
        sinCriterios="Mostrando todas las opciones de respuesta activas"
        resumen={
          cargando
            ? "Buscando…"
            : error
              ? ""
              : `${opciones.length} ${opciones.length === 1 ? "mostrada" : "mostradas"}`
        }
      />

      <Surface bordered padding="none" layout={false}>
        <div className="px-5 py-4">
          <h2 className="font-semibold">Opciones de respuesta</h2>
          <p className={styles.muted}>
            Seleccioná una opción de respuesta para modificarla o eliminarla.
          </p>
        </div>

        {cargando && opciones.length === 0 ? (
          <div className={styles.status} role="status">
            <Spinner size="sm" aria-hidden="true" />
            <span>Cargando opciones de respuesta…</span>
          </div>
        ) : opciones.length ? (
          <>
            <div
              className={`${styles.tableScroll} mx-4 mb-4`}
              tabIndex={0}
              aria-label="Listado de opciones de respuesta"
              data-cargando={cargando}
              aria-busy={cargando}
            >
              <table className={styles.table}>
                <caption className="sr-only">Opciones de respuesta disponibles.</caption>
                <thead>
                  <tr>
                    <th scope="col" className={styles.radioCell}>
                      <span className="sr-only">Seleccionar</span>
                    </th>
                    {["Nombre", "Descripción"].map((titulo) => (
                      <th key={titulo} scope="col">
                        {titulo}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {opciones.map((opcion) => {
                    const estaSeleccionada = selectedId === opcion.id;

                    return (
                      <tr
                        key={opcion.id}
                        className={`${styles.selectableRow} ${
                          estaSeleccionada ? styles.selectedRow : ""
                        }`}
                        onClick={() => alternarSeleccion(opcion.id)}
                      >
                        <td
                          className={styles.radioCell}
                          onClick={(event) => event.stopPropagation()}
                        >
                          <RadioButton
                            label={<span className="sr-only">Seleccionar {opcion.nombre}</span>}
                            value={String(opcion.id)}
                            checked={estaSeleccionada}
                            onCheckedChange={() => {
                              if (!estaSeleccionada) alternarSeleccion(opcion.id);
                            }}
                            onClick={() => {
                              if (estaSeleccionada) alternarSeleccion(opcion.id);
                            }}
                            size="sm"
                          />
                        </td>
                        <td>
                          <span className={styles.opcionName}>{opcion.nombre}</span>
                        </td>
                        <td className={styles.opcionDescripcion}>{opcion.descripcion || "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {cursor !== null && !cargando && (
              <div className="flex justify-center pb-4">
                <Button
                  variant="secondary"
                  loading={cargandoMas}
                  disabled={cargandoMas}
                  onClick={() => void cargarMas()}
                >
                  Cargar más opciones de respuesta
                </Button>
              </div>
            )}

            <AnimatePresence initial={false}>
              {opcionSeleccionada && (
                <motion.div
                  key="barra-seleccion"
                  className={styles.selectionBarClip}
                  variants={reducirMovimiento ? barraVariantsReducidas : barraVariants}
                  initial="oculta"
                  animate="visible"
                  exit="oculta"
                >
                  <div
                    className={styles.selectionBar}
                    role="region"
                    aria-label="Acciones sobre la opción de respuesta seleccionada"
                  >
                    <motion.div
                      variants={reducirMovimiento ? accionesVariantsReducidas : accionesVariants}
                    >
                      <Button
                        type="button"
                        variant="secondary"
                        size="sm"
                        onClick={() => setSelectedId(null)}
                      >
                        Limpiar selección
                      </Button>
                    </motion.div>

                    <motion.div
                      className={styles.selectionActions}
                      variants={reducirMovimiento ? accionesVariantsReducidas : accionesVariants}
                    >
                      <AccionEnDesarrolloModal
                        etiquetaBoton="Modificar"
                        variant="secondary"
                        titulo="Modificar Opción de Respuesta"
                        nombreRecurso={opcionSeleccionada.nombre}
                        detalle="La modificación de opciones de respuesta estará disponible próximamente."
                      />
                      <AccionEnDesarrolloModal
                        etiquetaBoton="Eliminar"
                        variant="destructive"
                        titulo="Dar de baja Opción de Respuesta"
                        nombreRecurso={opcionSeleccionada.nombre}
                        detalle="La baja de opciones de respuesta estará disponible próximamente."
                      />
                    </motion.div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </>
        ) : (
          !error && (
            <div className={styles.empty}>
              <span className={styles.iconBox}>
                <Icon name="list" width="26" height="26" />
              </span>
              <h3 className="text-lg font-semibold">
                {hayFiltros ? "Sin coincidencias" : "Todavía no hay opciones de respuesta"}
              </h3>
              <p className={`${styles.muted} max-w-lg`}>
                {hayFiltros
                  ? "No se encontraron resultados para los criterios ingresados."
                  : "Aún no se registraron opciones de respuesta activas en el catálogo."}
              </p>
              {hayFiltros ? (
                <Button type="button" variant="secondary" onClick={limpiar}>
                  Limpiar filtros
                </Button>
              ) : (
                <AltaOpcionRespuestaModal onCreada={recargar} />
              )}
            </div>
          )
        )}
      </Surface>
    </Stack>
  );
}
