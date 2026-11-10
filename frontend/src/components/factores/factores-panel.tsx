"use client";

import { SyntheticEvent, useCallback, useEffect, useRef, useState } from "react";
import { Button, RadioButton, Spinner, Stack, Surface, TextField } from "react-ui-lib";
import { PageHeader } from "../ui/page-header";
import { Icon } from "../ui/icon";
import { useErrorFeedback } from "../ui/use-error-feedback";
import { AgregarFactorModal } from "./agregar-factor-modal";
import { ModificarFactorModal } from "./modificar-factor-modal";
import { EliminarFactorModal } from "./eliminar-factor-modal";
import { listarFactores } from "@/service/factores";
import type { Factor, FiltrosFactores } from "@/service/factores";
import styles from "./factores.module.css";

const FILTROS_VACIOS: FiltrosFactores = { nombreCompetencia: "", nombreFactor: "" };

export function FactoresPanel() {
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);
  const [consulta, setConsulta] = useState({ filtros: FILTROS_VACIOS, revision: 0 });
  const [factores, setFactores] = useState<Factor[]>([]);
  const [seleccionadosIds, setSeleccionadosIds] = useState<number[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [cargando, setCargando] = useState(true);
  const [cargandoMas, setCargandoMas] = useState(false);
  const [error, setError] = useState(false);
  const siguientePeticion = useRef<AbortController | null>(null);

  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({
    retrying: cargando || cargandoMas,
  });

  const hayFiltros = Boolean(
    consulta.filtros.nombreCompetencia.trim() || consulta.filtros.nombreFactor.trim()
  );

  const factoresSeleccionados = factores.filter(
    (f) => typeof f.id === "number" && seleccionadosIds.includes(f.id)
  );

  const idsDisponibles = factores
    .map((f) => f.id)
    .filter((id): id is number => typeof id === "number");

  const todosSeleccionados =
    idsDisponibles.length > 0 &&
    idsDisponibles.every((id) => seleccionadosIds.includes(id));

  const recargar = useCallback(() => {
    setCargando(true);
    setError(false);
    setConsulta((anterior) => ({ ...anterior, revision: anterior.revision + 1 }));
  }, []);

  useEffect(() => {
    const controller = new AbortController();

    void listarFactores(consulta.filtros, null, controller.signal)
      .then((resultado) => {
        if (controller.signal.aborted) return;
        setFactores(resultado.factores);
        setCursor(resultado.siguienteCursor);
        const idsRecibidos = new Set(
          resultado.factores
            .map((f) => f.id)
            .filter((id): id is number => typeof id === "number")
        );
        setSeleccionadosIds((prev) => prev.filter((id) => idsRecibidos.has(id)));
        clearError();
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        setFactores([]);
        setSeleccionadosIds([]);
        setCursor(null);
        setError(true);
        reportError(error, {
          title: "No pudimos completar la consulta",
          fallback: "No pudimos cargar los factores.",
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
    setSeleccionadosIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function alternarSeleccionTodos() {
    if (todosSeleccionados) {
      setSeleccionadosIds([]);
    } else {
      setSeleccionadosIds(idsDisponibles);
    }
  }

  function buscar(event: SyntheticEvent<HTMLFormElement>) {
    event.preventDefault();
    setCargando(true);
    setError(false);
    setSeleccionadosIds([]);
    setConsulta((anterior) => ({ filtros, revision: anterior.revision + 1 }));
  }

  function limpiar() {
    setFiltros(FILTROS_VACIOS);
    setCargando(true);
    setError(false);
    setSeleccionadosIds([]);
    setConsulta((anterior) => ({ filtros: FILTROS_VACIOS, revision: anterior.revision + 1 }));
  }

  async function cargarMas() {
    if (cursor === null || cargando || siguientePeticion.current) return;

    const controller = new AbortController();
    siguientePeticion.current = controller;
    setCargandoMas(true);
    setError(false);

    try {
      const resultado = await listarFactores(consulta.filtros, cursor, controller.signal);
      if (controller.signal.aborted) return;
      setFactores((anteriores) => [...anteriores, ...resultado.factores]);
      setCursor(resultado.siguienteCursor);
    } catch (error) {
      if (controller.signal.aborted) return;
      reportError(error, {
        title: "No pudimos cargar más factores",
        fallback: "Ocurrió un error al cargar los siguientes factores.",
      });
    } finally {
      if (siguientePeticion.current === controller) siguientePeticion.current = null;
      if (!controller.signal.aborted) setCargandoMas(false);
    }
  }

  if (hasRetryError) return feedback;

  return (
    <Stack gap="lg" className={styles.theme}>
      <PageHeader
        title="Gestor de Factores"
        description="Creá, modificá y visualizá los factores disponibles para clasificar preguntas."
      />

      <Surface bordered>
        <form className={styles.filters} onSubmit={buscar} noValidate aria-label="Buscar factores">
          <TextField
            label="Nombre de competencia"
            name="Competencia"
            value={filtros.nombreCompetencia}
            onChange={(nombreCompetencia) => setFiltros({ ...filtros, nombreCompetencia })}
            maxLength={75}
            placeholder="Buscar por nombre de competencia"
          />
          <TextField
            label="Nombre de factor"
            name="Factor"
            value={filtros.nombreFactor}
            onChange={(nombreFactor) => setFiltros({ ...filtros, nombreFactor })}
            maxLength={50}
            placeholder="Buscar por nombre de factor"
          />
          <Button type="submit" disabled={cargando} className="mb-2">
            Buscar
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={cargando}
            onClick={limpiar}
            className="mb-2"
          >
            Limpiar
          </Button>
        </form>
      </Surface>

      <Surface bordered padding="none">
        <Stack direction="row" justify="between" gap="md" wrap className="px-5 py-4">
          <div>
            <h2 className="font-semibold">Factores</h2>
            <p className={styles.muted}>
              Seleccioná uno para modificar o eliminar, o varios para eliminar en conjunto.
            </p>
          </div>
          <Stack direction="column" align="end" gap="sm" wrap>
            <AgregarFactorModal onCreado={recargar} />
            {!cargando && !error && (
              <span className={styles.muted + " pr-1"} aria-live="polite">
                {factores.length} {factores.length === 1 ? "mostrado" : "mostrados"}
              </span>
            )}
          </Stack>
        </Stack>

        {cargando ? (
          <div className={styles.status} role="status">
            <Spinner size="sm" aria-hidden="true" />
            <span>Cargando factores…</span>
          </div>
        ) : factores.length ? (
          <>
            <div
              className={`${styles.tableScroll} mx-4 mb-4`}
              tabIndex={0}
              aria-label="Listado de factores"
            >
              <table className={styles.table}>
                <caption className="sr-only">Factores disponibles.</caption>
                <thead>
                  <tr>
                    <th scope="col" className={styles.radioCell}>
                      <RadioButton
                        label={<span className="sr-only">Seleccionar todos los factores</span>}
                        value="todos"
                        checked={todosSeleccionados}
                        onCheckedChange={() => {
                          if (!todosSeleccionados) alternarSeleccionTodos();
                        }}
                        onClick={() => {
                          if (todosSeleccionados) alternarSeleccionTodos();
                        }}
                        size="sm"
                      />
                    </th>
                    {["Código", "Nombre", "Competencia", "Orden", "Descripción"].map((titulo) => (
                      <th key={titulo} scope="col">
                        {titulo}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {factores.map((factor) => {
                    const estaSeleccionado =
                      typeof factor.id === "number" && seleccionadosIds.includes(factor.id);

                    return (
                      <tr
                        key={factor.id ?? factor.codigo}
                        className={`${styles.selectableRow} ${
                          estaSeleccionado ? styles.selectedRow : ""
                        }`}
                        onClick={() => alternarSeleccion(factor.id)}
                      >
                        <td
                          className={styles.radioCell}
                          onClick={(event) => event.stopPropagation()}
                        >
                          <RadioButton
                            label={
                              <span className="sr-only">Seleccionar {factor.nombre}</span>
                            }
                            value={String(factor.id ?? factor.codigo)}
                            checked={estaSeleccionado}
                            onCheckedChange={() => {
                              if (!estaSeleccionado) alternarSeleccion(factor.id);
                            }}
                            onClick={() => {
                              if (estaSeleccionado) alternarSeleccion(factor.id);
                            }}
                            size="sm"
                          />
                        </td>
                        <td className="tabular-nums font-medium">{factor.codigo}</td>
                        <td>
                          <span className={styles.factorName}>{factor.nombre}</span>
                        </td>
                        <td>{factor.nombreCompetencia ?? `Competencia #${factor.competenciaId}`}</td>
                        <td className="tabular-nums">{factor.orden}</td>
                        <td className={styles.summary}>{factor.descripcion || "—"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {cursor !== null && (
              <div className="flex justify-center pb-4">
                <Button
                  variant="secondary"
                  loading={cargandoMas}
                  disabled={cargandoMas}
                  onClick={() => void cargarMas()}
                >
                  Cargar más factores
                </Button>
              </div>
            )}

            {factoresSeleccionados.length > 0 && (
              <div
                className={styles.selectionBar}
                role="region"
                aria-label="Acciones sobre factores seleccionados"
              >
                <Stack direction="row" align="center" gap="sm" wrap>
                  <span className="text-sm font-medium">
                    {factoresSeleccionados.length === 1
                      ? `1 factor seleccionado: ${factoresSeleccionados[0].nombre}`
                      : `${factoresSeleccionados.length} factores seleccionados`}
                  </span>
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    onClick={() => setSeleccionadosIds([])}
                  >
                    Limpiar selección
                  </Button>
                </Stack>

                <Stack direction="row" align="center" gap="sm" wrap>
                  {factoresSeleccionados.length === 1 && (
                    <ModificarFactorModal
                      factor={factoresSeleccionados[0]}
                      onModificado={() => {
                        setSeleccionadosIds([]);
                        recargar();
                      }}
                    />
                  )}
                  <EliminarFactorModal
                    factores={factoresSeleccionados}
                    onEliminado={() => {
                      setSeleccionadosIds([]);
                      recargar();
                    }}
                  />
                </Stack>
              </div>
            )}
          </>
        ) : (
          !error && (
            <div className={styles.empty}>
              <span className={styles.uploadIcon}>
                <Icon name="layers" width="26" height="26" />
              </span>
              <h3 className="text-lg font-semibold">
                {hayFiltros ? "Sin coincidencias" : "Todavía no hay factores"}
              </h3>
              <p className={`${styles.muted} max-w-lg`}>
                {hayFiltros
                  ? "Probá con otros datos o limpiá los filtros para ver todos los factores activos."
                  : "Creá tu primer factor desde el botón Agregar factor."}
              </p>
            </div>
          )
        )}
      </Surface>
    </Stack>
  );
}
