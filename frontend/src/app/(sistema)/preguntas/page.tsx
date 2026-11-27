"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button, Spinner, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { ApiError } from "@/service/api";
import {
  Competencia,
  Factor,
  FiltrosPreguntas,
  ListadoPregunta,
  listarCompetencias,
  listarFactoresPorCompetencia,
  listarPreguntas,
} from "@/service/preguntas";

const TAMANO_PAGINA = 10;

export default function PreguntasPage() {
  const { toast } = useToast();

  const [competencias, setCompetencias] = useState<Competencia[]>([]);
  const [factores, setFactores] = useState<Factor[]>([]);
  const [preguntas, setPreguntas] = useState<ListadoPregunta[]>([]);

  const [competenciaId, setCompetenciaId] = useState<string>("");
  const [factorId, setFactorId] = useState<string>("");
  const [nombre, setNombre] = useState<string>("");
  const [activa, setActiva] = useState<string>("");

  const [pagina, setPagina] = useState<number>(0);
  const [cargando, setCargando] = useState<boolean>(true);
  const [hayMas, setHayMas] = useState<boolean>(false);

  // Cargar competencias al montar
  useEffect(() => {
    let activo = true;
    listarCompetencias()
      .then((datos) => {
        if (activo) setCompetencias(datos);
      })
      .catch(() => {
        if (activo) {
          toast({ title: "No se pudieron cargar las competencias"});
        }
      });
    return () => {
      activo = false;
    };
  }, [toast]);

  // Carga inicial de preguntas
  useEffect(() => {
    let activo = true;
    listarPreguntas({ offset: 0, cantidad: TAMANO_PAGINA + 1 })
      .then((resultado) => {
        if (!activo) return;
        setHayMas(resultado.length > TAMANO_PAGINA);
        setPreguntas(resultado.slice(0, TAMANO_PAGINA));
      })
      .catch((error) => {
        if (!activo) return;
        const mensaje = error instanceof ApiError ? error.message : "Error al consultar preguntas.";
        toast({ title: mensaje });
      })
      .finally(() => {
        if (activo) setCargando(false);
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

  const cargarPreguntas = async (nuevaPagina: number, filtrosPersonalizados?: FiltrosPreguntas) => {
    setCargando(true);
    try {
      const filtros: FiltrosPreguntas = filtrosPersonalizados ?? {
        competenciaId: competenciaId ? Number(competenciaId) : undefined,
        factorId: factorId ? Number(factorId) : undefined,
        nombre: nombre || undefined,
        activa: activa === "" ? undefined : activa === "true",
      };

      const resultado = await listarPreguntas({
        ...filtros,
        offset: nuevaPagina * TAMANO_PAGINA,
        cantidad: TAMANO_PAGINA + 1,
      });

      setHayMas(resultado.length > TAMANO_PAGINA);
      setPreguntas(resultado.slice(0, TAMANO_PAGINA));
      setPagina(nuevaPagina);
    } catch (error) {
      const mensaje = error instanceof ApiError ? error.message : "Error al consultar preguntas.";
      toast({ title: mensaje});
    } finally {
      setCargando(false);
    }
  };

  const handleBuscar = (e: React.FormEvent) => {
    e.preventDefault();
    cargarPreguntas(0);
  };

  const handleLimpiar = () => {
    setCompetenciaId("");
    setFactorId("");
    setFactores([]);
    setNombre("");
    setActiva("");
    cargarPreguntas(0, {});
  };

  return (
    <Stack gap="lg">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Gestión de Preguntas</h1>
          <p className="text-sm opacity-75">Consultá, filtrá y administrá el banco de preguntas por factor.</p>
        </div>
        <Link href="/preguntas/nueva">
          <Button>Nueva pregunta</Button>
        </Link>
      </div>

      {/* Filtros */}
      <Surface className="p-4">
        <form onSubmit={handleBuscar} className="grid grid-cols-1 gap-4 md:grid-cols-4">
          <div className="flex flex-col gap-1">
            <label htmlFor="filtro-competencia" className="text-sm font-medium">
              Competencia
            </label>
            <select
              id="filtro-competencia"
              value={competenciaId}
              onChange={(e) => handleCompetenciaChange(e.target.value)}
              className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm"
            >
              <option value="">Todas</option>
              {competencias.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1">
            <label htmlFor="filtro-factor" className="text-sm font-medium">
              Factor
            </label>
            <select
              id="filtro-factor"
              value={factorId}
              onChange={(e) => setFactorId(e.target.value)}
              disabled={!competenciaId}
              className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm disabled:opacity-50"
            >
              <option value="">Todos</option>
              {factores.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nombre}
                </option>
              ))}
            </select>
          </div>

          <TextField
            label="Nombre"
            placeholder="Buscar por nombre..."
            value={nombre}
            onChange={setNombre}
          />

          <div className="flex flex-col gap-1">
            <label htmlFor="filtro-estado" className="text-sm font-medium">
              Estado
            </label>
            <select
              id="filtro-estado"
              value={activa}
              onChange={(e) => setActiva(e.target.value)}
              className="rounded-md border border-[var(--ui-border)] bg-[var(--ui-surface)] px-3 py-2 text-sm"
            >
              <option value="">Todos</option>
              <option value="true">Activas</option>
              <option value="false">Inactivas</option>
            </select>
          </div>

          <div className="flex items-end gap-2 md:col-span-4">
            <Button type="submit">Buscar</Button>
            <Button type="button" variant="secondary" onClick={handleLimpiar}>
              Limpiar
            </Button>
          </div>
        </form>
      </Surface>

      {/* Tabla paginada */}
      <Surface className="overflow-x-auto p-4">
        {cargando ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : preguntas.length === 0 ? (
          <p className="py-8 text-center text-sm opacity-75">
            No se encontraron preguntas con los filtros seleccionados.
          </p>
        ) : (
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-[var(--ui-border)]">
                <th className="px-3 py-2 font-semibold">Nombre</th>
                <th className="px-3 py-2 font-semibold">Enunciado</th>
                <th className="px-3 py-2 font-semibold">Competencia</th>
                <th className="px-3 py-2 font-semibold">Factor</th>
                <th className="px-3 py-2 font-semibold">Opción de Respuesta</th>
                <th className="px-3 py-2 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              {preguntas.map((p) => (
                <tr key={p.id} className="border-b border-[var(--ui-border)] last:border-b-0">
                  <td className="px-3 py-2 font-medium">{p.nombre}</td>
                  <td className="px-3 py-2">{p.enunciado}</td>
                  <td className="px-3 py-2">{p.nombreCompetencia}</td>
                  <td className="px-3 py-2">{p.nombreFactor}</td>
                  <td className="px-3 py-2">{p.nombreOpcionRespuesta}</td>
                  <td className="px-3 py-2">{p.activa ? "Activa" : "Inactiva"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="mt-4 flex items-center justify-between border-t border-[var(--ui-border)] pt-3 text-sm">
          <span>Página {pagina + 1}</span>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              disabled={pagina === 0 || cargando}
              onClick={() => cargarPreguntas(pagina - 1)}
            >
              Anterior
            </Button>
            <Button
              variant="secondary"
              disabled={!hayMas || cargando}
              onClick={() => cargarPreguntas(pagina + 1)}
            >
              Siguiente
            </Button>
          </div>
        </div>
      </Surface>
    </Stack>
  );
}