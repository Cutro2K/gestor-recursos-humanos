"use client";

import { useEffect, useId, useRef, useState } from "react";
import type { DragEvent } from "react";
import { Button, Modal, Spinner, Stack, Surface, useToast } from "react-ui-lib";
import { Feedback } from "@/components/ui/feedback";
import { Icon } from "@/components/ui/icon";
import { TextLink } from "@/components/ui/text-link";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { importarCandidatos, MAX_CSV_BYTES, previsualizarCandidatos } from "@/service/candidatos";
import type { Previsualizacion, ResultadoImportacion } from "@/service/candidatos";
import { PreviewImportacion, ReporteErrores, ResumenOperaciones, ResumenProblemas } from "./preview-importacion";
import styles from "./candidatos.module.css";
import layout from "./importacion-layout.module.css";

type Estado =
  | { paso: "archivo" }
  | { paso: "analizando"; archivo: File }
  | { paso: "preview" | "importando"; archivo: File; preview: Previsualizacion }
  | { paso: "resultado"; resultado: ResultadoImportacion };

export function ImportarCandidatosModal({ onImportado }: { onImportado: () => void }) {
  const [abierto, setAbierto] = useState(false);
  const [estado, setEstado] = useState<Estado>({ paso: "archivo" });
  const [arrastrando, setArrastrando] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const peticionRef = useRef<AbortController | null>(null);
  const arrastresRef = useRef(0);
  const seleccionId = useId();
  const toast = useToast();
  const importando = estado.paso === "importando";
  const ocupado = importando || estado.paso === "analizando";
  const preview = estado.paso === "preview" || importando ? estado.preview : null;
  const validas = preview ? preview.aCrear + preview.aActualizar + preview.aEliminar : 0;
  const { reportError, feedback, hasRetryError, clearError } = useErrorFeedback({ retrying: ocupado });

  useEffect(() => () => peticionRef.current?.abort(), []);

  function cambiarApertura(valor: boolean) {
    if (importando) return;

    peticionRef.current?.abort();
    peticionRef.current = null;
    arrastresRef.current = 0;
    setArrastrando(false);

    if (valor) {
      setEstado({ paso: "archivo" });
      clearError();
    }

    setAbierto(valor);
  }

  async function seleccionarArchivos(archivos: FileList | null) {
    if (!archivos?.length || importando) return;

    peticionRef.current?.abort();
    peticionRef.current = null;
    setEstado({ paso: "archivo" });
    clearError();

    if (archivos.length !== 1) {
      toast.error("No se pudo procesar el archivo", "Seleccioná un solo archivo CSV por importación.");
      return;
    }

    const archivo = archivos[0];

    if (!archivo.name.toLowerCase().endsWith(".csv")) {
      toast.error("No se pudo procesar el archivo", "El archivo debe tener extensión .csv.");
      return;
    }

    if (!archivo.size || archivo.size > MAX_CSV_BYTES) {
      toast.error(
        "No se pudo procesar el archivo",
        archivo.size ? "El archivo supera el máximo de 2 MiB." : "El archivo está vacío.",
      );
      return;
    }

    await analizarArchivo(archivo);
  }

  async function analizarArchivo(archivo: File) {
    peticionRef.current?.abort();
    const controller = new AbortController();
    peticionRef.current = controller;
    setEstado({ paso: "analizando", archivo });

    try {
      const resultado = await previsualizarCandidatos(archivo, controller.signal);
      if (controller.signal.aborted) return;

      setEstado({ paso: "preview", archivo, preview: resultado });
      clearError();
      if (!resultado.filas.length) {
        toast.warning("El archivo no contiene candidatos", "Solo encontramos el encabezado. Agregá filas y volvé a cargarlo.");
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setEstado({ paso: "archivo" });
        reportError(error, {
          title: "No se pudo procesar el archivo",
          fallback: "No pudimos analizar el archivo.",
          retry: () => { void analizarArchivo(archivo); },
          retryLabel: "Volver a analizar",
        });
      }
    } finally {
      if (peticionRef.current === controller) peticionRef.current = null;
    }
  }

  async function confirmar() {
    if (estado.paso !== "preview" || !validas || peticionRef.current) return;

    const controller = new AbortController();
    peticionRef.current = controller;
    setEstado({ ...estado, paso: "importando" });

    try {
      const resultado = await importarCandidatos(estado.archivo, controller.signal);
      if (controller.signal.aborted) return;

      setEstado({ paso: "resultado", resultado });
      onImportado();

      const procesados = resultado.creados + resultado.actualizados + resultado.eliminados;
      if (resultado.rechazados || !procesados) {
        toast.warning("Importación finalizada", `${procesados} filas procesadas y ${resultado.rechazados} rechazadas.`);
      } else {
        toast.success("Importación completada", `${procesados} filas procesadas correctamente.`);
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setEstado({ paso: "archivo" });

        // La respuesta puede perderse después de guardar. Reanalizar evita repetir una escritura a ciegas.
        reportError(error, {
          title: "No pudimos confirmar el resultado de la importación",
          fallback: "No pudimos completar la importación.",
          context: "Volvé a analizar el archivo para revisar el estado actual antes de confirmar otra vez.",
          retry: () => { void analizarArchivo(estado.archivo); },
          retryLabel: "Volver a analizar",
        });
      }
    } finally {
      if (peticionRef.current === controller) peticionRef.current = null;
    }
  }

  function soltarArchivo(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    arrastresRef.current = 0;
    setArrastrando(false);
    void seleccionarArchivos(event.dataTransfer.files);
  }

  return (
    <Modal
      open={abierto}
      onOpenChange={cambiarApertura}
      title={estado.paso === "resultado" ? "Resultado de la importación" : "Importar candidatos"}
      description="Cargá un CSV, revisá las acciones previstas y confirmá la importación de las filas válidas."
      trigger={(
        <Button className={styles.importTrigger} leadingIcon={<Icon name="upload" />}>
          <span className={styles.importLabel}>Importar candidatos</span>
        </Button>
      )}
      className={`${styles.theme} ${layout.modal}`}
      style={{ maxWidth: "72rem", height: "min(52rem, calc(100dvh - 2rem))" }}
      size="lg"
      initialFocusId={seleccionId}
      closeOnEscape={!importando}
      closeOnOutsideClick={false}
      showCloseButton={!importando}
      actions={hasRetryError ? (
        <Button variant="secondary" onClick={() => cambiarApertura(false)}>Cancelar</Button>
      ) : estado.paso === "resultado" ? (
        <>
          <Button variant="secondary" onClick={() => cambiarApertura(true)}>Importar otro archivo</Button>
          <Button onClick={() => cambiarApertura(false)}>Cerrar</Button>
        </>
      ) : (
        <>
          <p className={styles.footerNote} aria-live="polite">
            {preview ? `${validas} filas listas para importar · ${preview.rechazadas} rechazadas` : "Todavía no se guardaron cambios."}
          </p>
          <Button variant="secondary" disabled={importando} onClick={() => cambiarApertura(false)}>Cancelar</Button>
          <Button loading={importando} disabled={!validas || ocupado} onClick={() => void confirmar()}>
            {importando ? "Importando…" : "Finalizar importación"}
          </Button>
        </>
      )}
    >
      {hasRetryError ? feedback : (
        <Stack gap="lg" className={layout.modalBody}>
          <ol className={styles.steps} aria-label="Pasos de importación">
            {["Seleccionar archivo", "Revisar filas", "Resultado"].map((etiqueta, index) => (
              <li
                key={etiqueta}
                aria-current={(estado.paso === "resultado" ? 2 : preview ? 1 : 0) === index ? "step" : undefined}
              >
                <span>{index + 1}</span>{etiqueta}
              </li>
            ))}
          </ol>

          <input
            ref={inputRef}
            type="file"
            accept=".csv,text/csv"
            className="sr-only"
            tabIndex={-1}
            aria-label="Archivo CSV de candidatos"
            disabled={ocupado}
            onChange={(event) => {
              void seleccionarArchivos(event.target.files);
              event.target.value = "";
            }}
          />

          {estado.paso === "archivo" && (
            <>
              <div
                className={styles.dropzone}
                data-dragging={arrastrando}
                role="group"
                aria-label="Carga de archivo CSV"
                onDragEnter={(event) => {
                  event.preventDefault();
                  arrastresRef.current += 1;
                  setArrastrando(true);
                }}
                onDragOver={(event) => { event.preventDefault(); event.dataTransfer.dropEffect = "copy"; }}
                onDragLeave={(event) => {
                  event.preventDefault();
                  arrastresRef.current = Math.max(0, arrastresRef.current - 1);
                  if (!arrastresRef.current) setArrastrando(false);
                }}
                onDrop={soltarArchivo}
              >
                <span className={styles.uploadIcon}><Icon name="upload" width="25" height="25" /></span>
                <div>
                  <p className="text-lg font-semibold">Arrastrá tu archivo CSV hasta acá</p>
                  <p className={styles.muted}>Un archivo de hasta 2 MiB, con las 11 columnas de candidatos.</p>
                </div>
                <Button id={seleccionId} variant="secondary" onClick={() => inputRef.current?.click()}>
                  Seleccionar archivo
                </Button>
              </div>
              <Stack direction="row" justify="between" align="start" gap="md" wrap>
                <p className={`${styles.muted} max-w-2xl`}>
                  Usá encabezados y separá las columnas con punto y coma (;). Las fechas van en formato aaaa-mm-dd.
                  La columna Eliminado indica la baja con True o conserva al candidato con False.
                </p>
                <TextLink href="/candidatos-ejemplo.csv" download>Descargar ejemplo CSV</TextLink>
              </Stack>
            </>
          )}

          {estado.paso === "analizando" && (
            <div className={styles.status} role="status" aria-live="polite">
              <Spinner size="sm" aria-hidden="true" />
              <span>Analizando {estado.archivo.name}…</span>
            </div>
          )}

          {preview && (estado.paso === "preview" || estado.paso === "importando") && (
            <>
              <Surface bordered padding="sm">
                <Stack direction="row" align="center" justify="between" gap="md" wrap>
                  <Stack direction="row" gap="sm" align="center" className="min-w-0">
                    <Icon name="file" className="shrink-0" />
                    <p className={styles.fileName}>{estado.archivo.name}</p>
                    <span className={`${styles.muted} shrink-0`}>{Math.ceil(estado.archivo.size / 1024)} KiB</span>
                  </Stack>
                  <Button variant="secondary" size="sm" disabled={importando} onClick={() => inputRef.current?.click()}>
                    Cambiar archivo
                  </Button>
                </Stack>
              </Surface>
              {importando && <p className={styles.muted} role="status">Guardando las filas válidas. Esperá a que termine.</p>}
              <PreviewImportacion preview={preview} />
              <p className={styles.muted}>
                Solo se guardarán las filas válidas. Las bajas conservarán el historial del candidato.
                Los candidatos ausentes del archivo seguirán sin cambios.
              </p>
            </>
          )}

          {estado.paso === "resultado" && (
            <>
              <Feedback
                title={estado.resultado.creados + estado.resultado.actualizados + estado.resultado.eliminados
                  ? "Importación completada" : "No se importaron candidatos"}
                variant={estado.resultado.rechazados ? "info" : "success"}
              >
                {estado.resultado.rechazados
                  ? "Las filas válidas se procesaron. Revisá las rechazadas, corregí el CSV y volvé a cargarlo."
                  : "Todas las filas se procesaron correctamente. El listado de candidatos ya se está actualizando."}
              </Feedback>
              <ResumenOperaciones
                crear={estado.resultado.creados}
                actualizar={estado.resultado.actualizados}
                eliminar={estado.resultado.eliminados}
                rechazar={estado.resultado.rechazados}
              />
              <ResumenProblemas resumen={estado.resultado.resumenErrores} />
              <ReporteErrores errores={estado.resultado.errores} />
            </>
          )}
        </Stack>
      )}
    </Modal>
  );
}
