"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, Modal, Surface, useToast } from "react-ui-lib";
import { ApiError } from "@/service/api";
import type { Candidato } from "@/service/candidatos";
import { descargarClavesExcel, evaluarCandidatos } from "@/service/evaluaciones";
import type { EvaluacionResultado, Puesto } from "@/service/evaluaciones";
import styles from "./evaluaciones.module.css";

/** Selector nativo de "Guardar como" (Chrome y Edge); Firefox y Safari no lo tienen. */
type SelectorGuardado = (opciones: {
  suggestedName: string;
  types: { description: string; accept: Record<string, string[]> }[];
}) => Promise<{
  createWritable: () => Promise<{ write: (datos: Blob) => Promise<void>; close: () => Promise<void> }>;
}>;

function selectorGuardado(): SelectorGuardado | null {
  const candidato = (window as unknown as { showSaveFilePicker?: SelectorGuardado }).showSaveFilePicker;
  return typeof candidato === "function" ? candidato.bind(window) : null;
}

function descargar(archivo: Blob, nombre: string) {
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement("a");
  enlace.href = url;
  enlace.download = nombre;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  // Se libera después: Safari cancela la descarga si la URL se revoca en el mismo tick.
  window.setTimeout(() => URL.revokeObjectURL(url), 10_000);
}

type PasoConfirmacionProps = {
  puesto: Puesto;
  candidatos: Candidato[];
  onVolver: () => void;
};

/**
 * Pasos 11 a 18 del CU 25: lista final de candidatos, [Finalizar] genera la evaluación y las
 * claves, y el sistema ofrece exportarlas a un archivo Excel en la carpeta que elija el actor.
 */
export function PasoConfirmacion({ puesto, candidatos, onVolver }: PasoConfirmacionProps) {
  const router = useRouter();
  const toast = useToast();
  const [generando, setGenerando] = useState(false);
  const [resultado, setResultado] = useState<EvaluacionResultado | null>(null);
  const [exportando, setExportando] = useState(false);
  const [preguntarExportacion, setPreguntarExportacion] = useState(false);
  const archivo = useRef<Promise<Blob> | null>(null);

  const ordenados = useMemo(
    () =>
      [...candidatos].sort(
        (a, b) =>
          a.apellido.localeCompare(b.apellido, "es") || a.nombre.localeCompare(b.nombre, "es") || a.id - b.id,
      ),
    [candidatos],
  );
  const claves = useMemo(
    () => new Map(resultado?.credenciales.map((c) => [c.candidatoId, c.claveIngreso]) ?? []),
    [resultado],
  );

  // Pide el Excel apenas se genera la evaluación: así, al elegir exportarlo, la descarga sale
  // dentro del mismo clic, que es lo que los navegadores exigen para guardar un archivo.
  useEffect(() => {
    if (!resultado) return;
    const pedido = descargarClavesExcel(resultado.evaluacionId);
    pedido.catch(() => undefined);
    archivo.current = pedido;
  }, [resultado]);

  /** Pasos 12 y 13. */
  async function finalizar() {
    setGenerando(true);
    try {
      setResultado(await evaluarCandidatos(puesto.id, candidatos.map((c) => c.id)));
      setPreguntarExportacion(true);
    } catch (error) {
      const mensaje = error instanceof Error ? error.message : "No se pudo generar la evaluación.";
      toast.error(
        error instanceof ApiError && error.status === 409
          ? "No se pueden evaluar los candidatos"
          : "No se pudo generar la evaluación",
        mensaje,
      );
    } finally {
      setGenerando(false);
    }
  }

  const cantidad = resultado?.credenciales.length ?? 0;
  const cuestionariosGenerados =
    cantidad === 1 ? "Se generó 1 cuestionario" : `Se generaron ${cantidad} cuestionarios`;

  /** Flujo 14.A: no exporta; las claves quedan a la vista para que el consultor pueda leerlas. */
  function noExportar() {
    setPreguntarExportacion(false);
    toast.success("Evaluación generada", `${cuestionariosGenerados} para ${puesto.nombre}.`);
  }

  /** Pasos 14 a 17; cancelar el selector de carpeta vuelve a la pregunta (16.A). */
  async function exportar() {
    if (!resultado) return;
    const nombre = resultado.nombreArchivoExcel;
    const guardar = selectorGuardado();
    setExportando(true);
    try {
      if (guardar) {
        const destino = await guardar({
          suggestedName: nombre,
          types: [{ description: "Libro de Excel 97-2003", accept: { "application/vnd.ms-excel": [".xls"] } }],
        });
        const datos = await (archivo.current ?? descargarClavesExcel(resultado.evaluacionId));
        const escritura = await destino.createWritable();
        await escritura.write(datos);
        await escritura.close();
      } else {
        descargar(await (archivo.current ?? descargarClavesExcel(resultado.evaluacionId)), nombre);
      }
      // Paso 18: el caso de uso finaliza y vuelve a la pantalla principal.
      toast.success("Cuestionarios generados correctamente", `Las claves se guardaron en el archivo ${nombre}.`);
      router.push("/");
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      archivo.current = null;
      toast.error(
        "No se pudo exportar el archivo",
        error instanceof Error ? error.message : "No pudimos guardar el archivo Excel.",
      );
    } finally {
      setExportando(false);
    }
  }

  return (
    <>
      <Surface as="section" bordered padding="none" layout={false} aria-labelledby="titulo-confirmacion">
        <div className={styles.encabezadoGrilla}>
          <div>
            <h2 id="titulo-confirmacion" className="font-semibold">Candidatos a evaluar</h2>
            <p className={styles.muted}>
              {puesto.nombre} — {puesto.empresa}
            </p>
          </div>
          <span className={styles.muted}>
            {candidatos.length} {candidatos.length === 1 ? "candidato" : "candidatos"}
          </span>
        </div>

        <div className={`${styles.tableScroll} mb-4`} tabIndex={0} aria-label="Lista final de candidatos a evaluar">
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col">Apellido</th>
                <th scope="col">Nombre</th>
                <th scope="col">Tipo de documento</th>
                <th scope="col">N.º de documento</th>
                <th scope="col">Clave de ingreso</th>
              </tr>
            </thead>
            <tbody>
              {ordenados.map((candidato) => {
                const clave = claves.get(candidato.id);
                return (
                  <tr key={candidato.id}>
                    <td>{candidato.apellido}</td>
                    <td>{candidato.nombre}</td>
                    <td>{candidato.tipoDocumento}</td>
                    <td className="tabular-nums">{candidato.nroDocumento}</td>
                    <td>
                      {clave ? (
                        <span className={styles.clave}>{clave}</span>
                      ) : (
                        <span className={styles.pendiente}>Se genera al finalizar</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Surface>

      {resultado && !preguntarExportacion && (
        <div className={styles.accionesPaso}>
          <Button variant="secondary" onClick={() => router.push("/")}>
            Ir al inicio
          </Button>
          <Button onClick={() => setPreguntarExportacion(true)}>Exportar a Excel</Button>
        </div>
      )}

      {!resultado && (
        <div className={styles.accionesPaso}>
          <Button variant="secondary" onClick={onVolver} disabled={generando}>
            Volver
          </Button>
          <Button variant="secondary" onClick={() => router.push("/")} disabled={generando}>
            Cancelar
          </Button>
          <Button onClick={() => void finalizar()} loading={generando} disabled={generando}>
            {generando ? "Generando cuestionarios…" : "Finalizar"}
          </Button>
        </div>
      )}

      <Modal
        open={resultado !== null && preguntarExportacion}
        onOpenChange={() => undefined}
        closeOnEscape={false}
        closeOnOutsideClick={false}
        showCloseButton={false}
        title="Evaluación generada"
        description={`${cuestionariosGenerados} en estado activo. ¿Desea exportar la lista de candidatos con sus claves a un archivo Excel?`}
        actions={
          <>
            <Button variant="secondary" disabled={exportando} onClick={noExportar}>
              No
            </Button>
            <Button loading={exportando} disabled={exportando} onClick={() => void exportar()}>
              {exportando ? "Exportando…" : "Sí, exportar"}
            </Button>
          </>
        }
      >
        <p className={styles.muted}>
          El archivo se llamará <strong>{resultado?.nombreArchivoExcel}</strong>.
        </p>
      </Modal>
    </>
  );
}
