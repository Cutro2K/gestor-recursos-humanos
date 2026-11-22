"use client";

import { useId } from "react";
import type { CSSProperties, FormEvent, ReactNode } from "react";
import { Button, Surface } from "react-ui-lib";
import styles from "./criterios-busqueda.module.css";

export type CriterioAplicado = {
  /** Identifica el criterio; también sirve de key. */
  clave: string;
  etiqueta: string;
  /** Lo que se muestra en la ficha, ya formateado (por ejemplo, entre comillas). */
  valor: ReactNode;
  onQuitar: () => void;
};

type CriteriosBusquedaProps = {
  /** Campos de texto de la primera fila. */
  campos: ReactNode;
  /** Columnas de la fila de campos (grid-template-columns). Por defecto, todas iguales. */
  columnas?: string;
  /** Controles que van a la izquierda de los botones, por ejemplo un RadioGroup. */
  opciones?: ReactNode;
  onBuscar: () => void;
  onLimpiar: () => void;
  puedeLimpiar: boolean;
  /** Criterios de la última búsqueda ejecutada (no lo que está escrito en el formulario). */
  aplicados: CriterioAplicado[];
  /** Texto de la derecha del resumen, por ejemplo la cantidad de resultados. */
  resumen: ReactNode;
  /** Texto cuando no hay criterios aplicados. */
  sinCriterios?: string;
};

/**
 * Bloque "Criterios de búsqueda" de las pantallas de gestión: un campo por criterio y
 * [Buscar] / [Limpiar], como en el diseño de interfaces (CU008, CU020). Debajo muestra
 * los criterios aplicados como fichas que se pueden quitar.
 */
export function CriteriosBusqueda({
  campos,
  columnas,
  opciones,
  onBuscar,
  onLimpiar,
  puedeLimpiar,
  aplicados,
  resumen,
  sinCriterios = "Mostrando todos los registros",
}: CriteriosBusquedaProps) {
  const tituloId = useId();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onBuscar();
  }

  const estiloCampos = columnas
    ? ({ "--columnas-busqueda": columnas } as CSSProperties)
    : undefined;

  return (
    // layout={false}: cada búsqueda cambia alturas, y la animación de layout deformaba el contenido.
    <Surface as="section" bordered padding="none" layout={false} aria-labelledby={tituloId}>
      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <h2 id={tituloId} className={styles.titulo}>
          Criterios de búsqueda
        </h2>
        <div className={styles.campos} style={estiloCampos}>
          {campos}
        </div>
        <div className={styles.pie}>
          <div className={styles.opciones}>{opciones}</div>
          <div className={styles.acciones}>
            <Button type="button" variant="secondary" onClick={onLimpiar} disabled={!puedeLimpiar}>
              Limpiar
            </Button>
            <Button type="submit">Buscar</Button>
          </div>
        </div>
      </form>

      <div className={styles.resumen}>
        <div className={styles.fichas} aria-label="Criterios aplicados">
          {aplicados.length > 0 ? (
            aplicados.map((criterio) => (
              <button
                key={criterio.clave}
                type="button"
                className={styles.ficha}
                onClick={criterio.onQuitar}
              >
                <span className={styles.fichaClave}>{criterio.etiqueta}</span>
                {criterio.valor}
                <span className={styles.fichaQuitar} aria-hidden="true">
                  ×
                </span>
                <span className="sr-only">Quitar criterio</span>
              </button>
            ))
          ) : (
            <span className={styles.apagado}>{sinCriterios}</span>
          )}
        </div>

        <span className={styles.cantidad} aria-live="polite">
          {resumen}
        </span>
      </div>
    </Surface>
  );
}
