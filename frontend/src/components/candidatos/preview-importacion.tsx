import { useState } from "react";
import { Button, Stack, Surface, Tabs } from "react-ui-lib";
import { COLUMNAS_CSV } from "@/service/candidatos";
import type {
  AccionImportacion, ErrorFila, FilaPrevisualizacion, Previsualizacion, ResumenErrores,
} from "@/service/candidatos";
import styles from "./candidatos.module.css";
import layout from "./importacion-layout.module.css";

const TAMANO_PAGINA = 25;

const ACCIONES: { accion: AccionImportacion; etiqueta: string }[] = [
  { accion: "CREAR", etiqueta: "Alta" },
  { accion: "ACTUALIZAR", etiqueta: "Actualización" },
  { accion: "ELIMINAR", etiqueta: "Baja" },
  { accion: "RECHAZAR", etiqueta: "Rechazada" },
];

export function ResumenOperaciones({
  crear, actualizar, eliminar, rechazar,
}: { crear: number; actualizar: number; eliminar: number; rechazar: number }) {
  const cantidades = [crear, actualizar, eliminar, rechazar];
  const etiquetas = ["Altas", "Actualizaciones", "Bajas", "Rechazadas"];

  return (
    <div className={styles.summary} role="group" aria-label="Resumen de operaciones">
      {ACCIONES.map(({ accion }, index) => (
        <Surface key={accion} bordered padding="sm" className={styles.summaryCard} data-action={accion}>
          <span className={styles.summaryNumber}>{cantidades[index]}</span>
          <span className={styles.muted}>{etiquetas[index]}</span>
        </Surface>
      ))}
    </div>
  );
}

export function ResumenProblemas({ resumen }: { resumen: ResumenErrores }) {
  if (!resumen.filasConErrores) return null;

  return (
    <section className={styles.errorSummary} aria-label="Resumen de errores por campo">
      <p className="text-sm font-semibold">
        {resumen.filasConErrores} {resumen.filasConErrores === 1 ? "fila con problemas" : "filas con problemas"}
      </p>
      <ul className={styles.errorFields}>
        {Object.entries(resumen.filasPorCampo).map(([campo, cantidad]) => (
          <li key={campo}><strong>{campo}:</strong> {cantidad}</li>
        ))}
      </ul>
      <p className={`${styles.muted} mt-2`}>Una fila puede tener problemas en varios campos.</p>
    </section>
  );
}

export function PreviewImportacion({ preview }: { preview: Previsualizacion }) {
  const cantidades = [preview.aCrear, preview.aActualizar, preview.aEliminar, preview.rechazadas];
  const etiquetas = ["Altas", "Actualizaciones", "Bajas", "Rechazadas"];

  return (
    <div className={layout.preview}>
      <Stack gap="lg">
        <ResumenOperaciones
          crear={preview.aCrear}
          actualizar={preview.aActualizar}
          eliminar={preview.aEliminar}
          rechazar={preview.rechazadas}
        />
        <ResumenProblemas resumen={preview.resumenErrores} />
      </Stack>
      <Tabs
        aria-label="Filtrar filas por acción"
        className={layout.previewTabs}
        listClassName={`${styles.tabsList} ${layout.tabsList}`}
        contentClassName={layout.previewPanel}
        tabs={[
          {
            value: "todas",
            label: `Todas (${preview.filas.length})`,
            content: <TablaPreview filas={preview.filas} />,
          },
          ...ACCIONES.map(({ accion }, index) => ({
            value: accion,
            label: `${etiquetas[index]} (${cantidades[index]})`,
            content: <TablaPreview filas={preview.filas.filter((fila) => fila.accion === accion)} />,
          })),
        ]}
      />
    </div>
  );
}

function TablaPreview({ filas }: { filas: FilaPrevisualizacion[] }) {
  const [pagina, setPagina] = useState(0);
  const visibles = filas.slice(pagina * TAMANO_PAGINA, (pagina + 1) * TAMANO_PAGINA);

  return (
    <div className={layout.previewRows}>
      {!filas.length ? (
        <p className={`${styles.muted} ${layout.previewEmpty}`} role="status">No hay filas en esta categoría.</p>
      ) : (
        <div className={`${styles.tableScroll} ${layout.previewTableScroll}`} tabIndex={0} aria-label="Previsualización de candidatos">
          <table className={styles.table}>
            <caption className="sr-only">Filas del archivo y acciones previstas. Abrí el detalle para ver los 11 campos.</caption>
            <thead>
              <tr>{["Fila", "Acción", "Candidato", "Documento", "Email", "Detalle"].map((titulo) => (
                <th key={titulo} scope="col">{titulo}</th>
              ))}</tr>
            </thead>
            <tbody>
              {visibles.map((fila) => (
                <tr key={fila.fila} className={styles.previewRow} data-action={fila.accion}>
                  <td>{fila.fila}</td>
                  <td>
                    <span className={styles.badge} data-action={fila.accion}>
                      {ACCIONES.find(({ accion }) => accion === fila.accion)?.etiqueta}
                    </span>
                  </td>
                  <td>
                    <span className={styles.candidateName}>
                      {[fila.valores.Apellido, fila.valores.Nombre].filter(Boolean).join(", ") || "Sin nombre"}
                    </span>
                    <span className={styles.muted}>N.º {fila.valores["Nro candidato"] || "—"}</span>
                  </td>
                  <td className="whitespace-nowrap">
                    {fila.valores["Tipo documento"]} {fila.valores["Nro documento"] || "—"}
                  </td>
                  <td className={styles.email}>{fila.valores.Email || "—"}</td>
                  <td>
                    <details className={styles.details}>
                      <summary>
                        {fila.errores.length
                          ? `${fila.errores.length} ${fila.errores.length === 1 ? "problema" : "problemas"}`
                          : "Ver datos"}
                      </summary>
                      {fila.errores.length > 0 && (
                        <ul aria-label={`Errores en fila ${fila.fila}`}>
                          {fila.errores.map((error, index) => (
                            <li key={index}><strong>{error.campo}:</strong> {error.mensaje}</li>
                          ))}
                        </ul>
                      )}
                      <dl>
                        {COLUMNAS_CSV.map((columna) => (
                          <div key={columna}>
                            <dt>{columna}</dt>
                            <dd>{fila.valores[columna] || "Sin valor"}</dd>
                          </div>
                        ))}
                      </dl>
                    </details>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Paginacion pagina={pagina} total={filas.length} onChange={setPagina} unidad="filas" />
    </div>
  );
}

export function ReporteErrores({ errores }: { errores: ErrorFila[] }) {
  const [pagina, setPagina] = useState(0);

  if (!errores.length) return null;

  return (
    <Stack gap="md">
      <h3 className="text-sm font-semibold">Errores por fila</h3>
      <div className={styles.tableScroll} tabIndex={0} aria-label="Reporte de filas rechazadas">
        <table className={styles.table}>
          <caption className="sr-only">Motivos por los que se rechazaron filas durante la importación.</caption>
          <thead><tr><th scope="col">Fila</th><th scope="col">Campo</th><th scope="col">Problema</th></tr></thead>
          <tbody>
            {errores.slice(pagina * TAMANO_PAGINA, (pagina + 1) * TAMANO_PAGINA).map((error, index) => (
              <tr key={`${error.fila}-${index}`}>
                <td>{error.fila}</td><td>{error.campo}</td><td>{error.mensaje}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginacion pagina={pagina} total={errores.length} onChange={setPagina} unidad="errores" />
    </Stack>
  );
}

function Paginacion({
  pagina, total, onChange, unidad,
}: { pagina: number; total: number; onChange: (pagina: number) => void; unidad: string }) {
  return (
    <Stack direction="row" align="center" justify="between" gap="md" wrap className={layout.pagination}>
      <p className={styles.muted} aria-live="polite">
        {total ? `${pagina * TAMANO_PAGINA + 1}–${Math.min((pagina + 1) * TAMANO_PAGINA, total)} de ${total}` : "0"} {unidad}
      </p>
      {total > TAMANO_PAGINA && (
        <Stack direction="row" gap="sm">
          <Button size="sm" variant="secondary" disabled={pagina === 0} onClick={() => onChange(pagina - 1)}>
            Anterior
          </Button>
          <Button
            size="sm" variant="secondary"
            disabled={(pagina + 1) * TAMANO_PAGINA >= total}
            onClick={() => onChange(pagina + 1)}
          >
            Siguiente
          </Button>
        </Stack>
      )}
    </Stack>
  );
}
