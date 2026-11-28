"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button, Spinner, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { ApiError } from "@/service/api";
import {
  Competencia,
  Factor,
  OpcionRespuesta,
  crearPregunta,
  listarCompetencias,
  listarFactoresPorCompetencia,
  listarOpcionesRespuesta,
} from "@/service/preguntas";

export default function NuevaPreguntaPage() {
  const router = useRouter();
  const { toast } = useToast();

  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [factores, setFactores] = useState<Factor[]>([]);
  const [opciones, setOpciones] = useState<OpcionRespuesta[]>([]);

  const [competenciaId, setCompetenciaId] = useState<string>("");
  const [factorId, setFactorId] = useState<string>("");
  const [opcionRespuestaId, setOpcionRespuestaId] = useState<string>("");
  const [nombre, setNombre] = useState<string>("");
  const [enunciado, setEnunciado] = useState<string>("");
  const [descripcion, setDescripcion] = useState<string>("");
  const [ponderaciones, setPonderaciones] = useState<Record<number, string>>({});

  const [cargandoCatalogos, setCargandoCatalogos] = useState<boolean>(true);
  const [guardando, setGuardando] = useState<boolean>(false);

  useEffect(() => {
    let activo = true;

    Promise.all([listarCompetencias(), listarOpcionesRespuesta()])
      .then(([comps, opcs]) => {
        if (!activo) return;
        setCompetencias(comps);
        setOpciones(opcs);
      })
      .catch(() => {
        if (!activo) return;
        toast({
          title: "Error al cargar catálogos",
          description: "Verificá que el backend esté activo y exponga competencias y opciones de respuesta.",
        });
      })
      .finally(() => {
        if (activo) setCargandoCatalogos(false);
      });

    return () => {
      activo = false;
    };
  }, [toast]);

  const handleCompetenciaChange = (nuevoId: string) => {
    setCompetenciaId(nuevoId);
    setFactorId("");
    if (!nuevoId) {
      setFactores([]);
      return;
    }
    listarFactoresPorCompetencia(Number(nuevoId))
      .then(setFactores)
      .catch(() => setFactores([]));
  };

  const handleOpcionRespuestaChange = (nuevoId: string) => {
    setOpcionRespuestaId(nuevoId);
    const opcion = opciones.find((o) => o.id === Number(nuevoId));
    if (!opcion) {
      setPonderaciones({});
      return;
    }
    const iniciales: Record<number, string> = {};
    for (const item of opcion.items) {
      iniciales[item.id] = "";
    }
    setPonderaciones(iniciales);
  };

  const opcionSeleccionada = useMemo(
    () => opciones.find((o) => o.id === Number(opcionRespuestaId)) ?? null,
    [opciones, opcionRespuestaId]
  );

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!factorId || !opcionSeleccionada || !nombre.trim() || !enunciado.trim()) {
      toast({ title: "Completá todos los campos obligatorios." });
      return;
    }

    const listaPonderaciones = opcionSeleccionada.items.map((item) => ({
      itemOpcionId: item.id,
      ponderacion: Number(ponderaciones[item.id]),
    }));

    const hayInvalido = listaPonderaciones.some(
      (p) => Number.isNaN(p.ponderacion) || p.ponderacion < 0 || ponderaciones[p.itemOpcionId]?.trim() === ""
    );

    if (hayInvalido) {
      toast({
        title: "Ponderaciones inválidas",
        description: "Ingresá un puntaje numérico mayor o igual a 0 para cada ítem.",
      });
      return;
    }

    setGuardando(true);
    try {
      await crearPregunta({
        factorId: Number(factorId),
        opcionRespuestaId: opcionSeleccionada.id,
        nombre: nombre.trim(),
        enunciado: enunciado.trim(),
        descripcion: descripcion.trim() || undefined,
        ponderaciones: listaPonderaciones,
      });

      toast({ title: "Pregunta registrada correctamente." });
      router.push("/preguntas");
    } catch (error) {
      const mensaje = error instanceof ApiError ? error.message : "No se pudo guardar la pregunta.";
      toast({ title: mensaje });
    } finally {
      setGuardando(false);
    }
  };

  if (cargandoCatalogos) {
    return (
      <div className="flex justify-center py-12">
        <Spinner />
      </div>
    );
  }

  return (
    <Stack gap="lg">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Nueva Pregunta</h1>
          <p className="text-sm opacity-75">
            Seleccioná la competencia, el factor, la opción de respuesta y asigná un puntaje a cada ítem.
          </p>
        </div>
        <Link href="/preguntas">
          <Button variant="secondary">Volver al listado</Button>
        </Link>
      </div>

      <Surface className="p-6">
        <form onSubmit={handleSubmit}>
          <Stack gap="md">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="flex flex-col gap-1">
                <label htmlFor="competencia" className="text-sm font-medium">
                  Competencia *
                </label>
                <select
                  id="competencia"
                  required
                  value={competenciaId}
                  onChange={(e) => handleCompetenciaChange(e.target.value)}
                  className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm"
                >
                  <option value="">Seleccioná una competencia...</option>
                  {competencias.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex flex-col gap-1">
                <label htmlFor="factor" className="text-sm font-medium">
                  Factor *
                </label>
                <select
                  id="factor"
                  required
                  value={factorId}
                  onChange={(e) => setFactorId(e.target.value)}
                  disabled={!competenciaId}
                  className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm disabled:opacity-50"
                >
                  <option value="">
                    {competenciaId ? "Seleccioná un factor..." : "Elegí primero una competencia"}
                  </option>
                  {factores.map((f) => (
                    <option key={f.id} value={f.id}>
                      {f.nombre}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <TextField
              label="Nombre *"
              placeholder="Ej: WBS y Alcance"
              maxLength={50}
              required
              value={nombre}
              onChange={setNombre}
            />

            <TextField
              label="Enunciado *"
              placeholder="Escribí la pregunta que verá el candidato..."
              maxLength={255}
              required
              value={enunciado}
              onChange={setEnunciado}
            />

            <div className="flex flex-col gap-1">
              <label htmlFor="descripcion" className="text-sm font-medium">
                Descripción
              </label>
              <textarea
                id="descripcion"
                rows={3}
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                placeholder="Descripción opcional para uso interno del consultor..."
                className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor="opcion-respuesta" className="text-sm font-medium">
                Opción de Respuesta *
              </label>
              <select
                id="opcion-respuesta"
                required
                value={opcionRespuestaId}
                onChange={(e) => handleOpcionRespuestaChange(e.target.value)}
                className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm"
              >
                <option value="">Seleccioná una plantilla de respuestas...</option>
                {opciones.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.nombre}
                  </option>
                ))}
              </select>
            </div>

            {opcionSeleccionada && (
              <div className="rounded-md border border-[var(--ui-border)] p-4">
                <h2 className="mb-3 text-sm font-semibold">Ponderación por ítem *</h2>
                <table className="w-full border-collapse text-left text-sm">
                  <thead>
                    <tr className="border-b border-[var(--ui-border)]">
                      <th className="px-2 py-2 font-medium">Orden</th>
                      <th className="px-2 py-2 font-medium">Opción</th>
                      <th className="px-2 py-2 font-medium">Puntaje (≥ 0)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[...opcionSeleccionada.items]
                      .sort((a, b) => a.orden - b.orden)
                      .map((item) => (
                        <tr key={item.id} className="border-b border-[var(--ui-border)] last:border-b-0">
                          <td className="px-2 py-2">{item.orden}</td>
                          <td className="px-2 py-2">{item.descripcion}</td>
                          <td className="px-2 py-2">
                            <input
                              type="number"
                              step="0.01"
                              min="0"
                              required
                              value={ponderaciones[item.id] ?? ""}
                              onChange={(e) =>
                                setPonderaciones((prev) => ({ ...prev, [item.id]: e.target.value }))
                              }
                              className="w-28 rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-2 py-1 text-sm"
                            />
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Link href="/preguntas">
                <Button type="button" variant="secondary" disabled={guardando}>
                  Cancelar
                </Button>
              </Link>
              <Button type="submit" disabled={guardando}>
                {guardando ? "Guardando..." : "Guardar pregunta"}
              </Button>
            </div>
          </Stack>
        </form>
      </Surface>
    </Stack>
  );
}