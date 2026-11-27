"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Button, Spinner, Stack, Surface, TextField } from "react-ui-lib";
import { PageHeader } from "@/components/ui/page-header";
import { useErrorFeedback } from "@/components/ui/use-error-feedback";
import { apiRequest } from "@/service/api";
import {
  generarEvaluacion,
  listarPuestos,
  type EvaluacionResponse,
  type Puesto,
} from "@/service/evaluaciones";
import type { Candidato, ListadoCandidatos } from "@/service/candidatos";

export function NuevaEvaluacionPanel() {
  const [puestos, setPuestos] = useState<Puesto[]>([]);
  const [candidatos, setCandidatos] = useState<Candidato[]>([]);
  const [puestoId, setPuestoId] = useState<string>("");
  const [seleccionados, setSeleccionados] = useState<number[]>([]);
  const [busqueda, setBusqueda] = useState("");
  const [cargando, setCargando] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [resultado, setResultado] = useState<EvaluacionResponse | null>(null);

  const { reportError, feedback, clearError } = useErrorFeedback({
    retrying: cargando,
  });

  const cargarDatos = useCallback(
    (signal?: AbortSignal) => {
      setCargando(true);
      clearError();
      Promise.all([
        listarPuestos(signal),
        apiRequest<ListadoCandidatos>("candidatos?limite=100", { signal }),
      ])
        .then(([puestosData, candidatosData]) => {
          if (signal?.aborted) return;
          setPuestos(puestosData);
          setCandidatos(candidatosData?.candidatos ?? []);
          if (puestosData.length > 0) {
            setPuestoId(String(puestosData[0].id));
          }
        })
        .catch((err) => {
          if (signal?.aborted) return;
          reportError(err, {
            title: "Error de carga",
            fallback: "No se pudieron cargar los puestos y candidatos.",
            retry: () => cargarDatos(),
          });
        })
        .finally(() => {
          if (!signal?.aborted) setCargando(false);
        });
    },
    [clearError, reportError],
  );

  useEffect(() => {
    const controller = new AbortController();
    cargarDatos(controller.signal);
    return () => controller.abort();
  }, [cargarDatos]);

  const candidatosFiltrados = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    if (!q) return candidatos;
    return candidatos.filter(
      (c) =>
        c.nombre.toLowerCase().includes(q) ||
        c.apellido.toLowerCase().includes(q) ||
        c.email.toLowerCase().includes(q),
    );
  }, [candidatos, busqueda]);

  const toggleCandidato = (id: number) => {
    setSeleccionados((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();
    setEnviando(true);
    try {
      const resp = await generarEvaluacion({
        puestoId: Number(puestoId),
        candidatoIds: seleccionados,
      });
      setResultado(resp);
    } catch (err) {
      reportError(err, {
        title: "Error al generar evaluación",
        fallback: "No se pudo generar la evaluación.",
      });
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Stack gap="lg">
      <PageHeader
        title="Nueva evaluación"
        description="Seleccioná un puesto y los candidatos a evaluar para generar sus cuestionarios (CU 25)."
        actions={
          <Link href="/evaluaciones">
            <Button variant="secondary">Volver a evaluaciones</Button>
          </Link>
        }
      />

      {feedback}

      {cargando ? (
        <Surface bordered style={{ minHeight: "220px", display: "grid", placeItems: "center" }}>
          <Spinner size="lg" />
        </Surface>
      ) : resultado ? (
        <Surface bordered padding="lg">
          <Stack gap="md">
            <h2 className="text-lg font-semibold">
              Evaluación #{resultado.evaluacionId} generada para {resultado.puestoNombre}
            </h2>
            <p className="text-sm opacity-80">
              Se generaron {resultado.cantidadCuestionarios} cuestionario(s) activos.
            </p>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm border-collapse">
                <thead>
                  <tr className="border-b">
                    <th className="py-2 pr-4">Candidato</th>
                    <th className="py-2 pr-4">Clave de ingreso</th>
                    <th className="py-2 pr-4">Estado</th>
                    <th className="py-2 pr-4">Bloques</th>
                    <th className="py-2">Preguntas</th>
                  </tr>
                </thead>
                <tbody>
                  {resultado.cuestionarios.map((c) => (
                    <tr key={c.cuestionarioId} className="border-b">
                      <td className="py-2 pr-4">
                        {c.candidatoApellido}, {c.candidatoNombre}
                      </td>
                      <td className="py-2 pr-4 font-mono font-semibold">{c.claveIngreso}</td>
                      <td className="py-2 pr-4">{c.estado}</td>
                      <td className="py-2 pr-4">{c.cantidadBloques}</td>
                      <td className="py-2">{c.cantidadPreguntas}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div>
              <Button
                variant="secondary"
                onClick={() => {
                  setResultado(null);
                  setSeleccionados([]);
                }}
              >
                Generar otra evaluación
              </Button>
            </div>
          </Stack>
        </Surface>
      ) : (
        <form onSubmit={handleSubmit}>
          <Stack gap="lg">
            <Surface bordered padding="lg">
              <Stack gap="md">
                <label htmlFor="puesto-select" className="text-sm font-medium">
                  Puesto a evaluar
                </label>
                <select
                  id="puesto-select"
                  value={puestoId}
                  onChange={(e) => setPuestoId(e.target.value)}
                  className="w-full rounded border px-3 py-2 text-sm bg-transparent"
                  required
                >
                  {puestos.length === 0 ? (
                    <option value="">No hay puestos activos</option>
                  ) : (
                    puestos.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.codigo} - {p.nombre} ({p.empresa})
                      </option>
                    ))
                  )}
                </select>
              </Stack>
            </Surface>

            <Surface bordered padding="lg">
              <Stack gap="md">
                <div className="flex items-center justify-between gap-4">
                  <span className="text-sm font-medium">
                    Candidatos ({seleccionados.length} seleccionados)
                  </span>
                  <div className="w-64">
                    <TextField
                      label="Buscar candidato"
                      value={busqueda}
                      onChange={(e) => setBusqueda(e.target.value)}
                      placeholder="Nombre, apellido o email..."
                    />
                  </div>
                </div>

                <div className="max-h-72 overflow-y-auto divide-y">
                  {candidatosFiltrados.map((c) => {
                    const checked = seleccionados.includes(c.id);
                    return (
                      <label
                        key={c.id}
                        className="flex items-center gap-3 py-2 cursor-pointer text-sm"
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => toggleCandidato(c.id)}
                        />
                        <span>
                          {c.apellido}, {c.nombre}
                        </span>
                        <span className="opacity-60 text-xs">{c.email}</span>
                      </label>
                    );
                  })}
                  {candidatosFiltrados.length === 0 && (
                    <p className="py-4 text-sm opacity-70">
                      No se encontraron candidatos con ese criterio.
                    </p>
                  )}
                </div>
              </Stack>
            </Surface>

            <div className="flex justify-end">
              <Button
                type="submit"
                disabled={enviando || puestos.length === 0 || seleccionados.length === 0}
              >
                {enviando ? "Generando cuestionarios..." : "Generar evaluación"}
              </Button>
            </div>
          </Stack>
        </form>
      )}
    </Stack>
  );
}
