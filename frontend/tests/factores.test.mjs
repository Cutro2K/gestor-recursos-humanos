import assert from "node:assert/strict";
import { register } from "node:module";
import { afterEach, mock, test } from "node:test";

register(new URL("./ts-resolver.mjs", import.meta.url));

const { API_BASE_URL, ApiError } = await import("../src/service/api.ts");
const {
  crearFactor,
  eliminarFactor,
  eliminarFactores,
  listarCompetenciasDisponibles,
  listarFactores,
  listarFactoresPorCompetencia,
  modificarFactor,
  obtenerFactorPorId,
} = await import("../src/service/factores.ts");

afterEach(() => mock.restoreAll());

test("listarCompetenciasDisponibles consulta GET /competencias", async () => {
  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, `${API_BASE_URL}/competencias`);
    return Response.json([
      { id: 1, nombre: "Gestión de proyectos" },
      { id: 2, nombre: "Liderazgo" },
    ]);
  });

  const competencias = await listarCompetenciasDisponibles();
  assert.equal(competencias.length, 2);
  assert.equal(competencias[0].nombre, "Gestión de proyectos");
});

test("listarFactores envía offset, cantidad y filtros de búsqueda y mapea ListadoGestorFactoresDTO", async () => {
  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(
      url,
      `${API_BASE_URL}/factores?offset=0&cantidad=50&nombreCompetencia=Gesti%C3%B3n&nombreFactor=An%C3%A1lisis`,
    );
    return Response.json([
      {
        id: 10,
        codigo: "FAC-01",
        nombreFactor: "Capacidad de análisis",
        competenciaId: 3,
        nombreCompetencia: "Gestión de proyectos",
        orden: 1,
        descripcion: "Pensamiento lógico y deductivo",
      },
    ]);
  });

  const resultado = await listarFactores(
    { nombreCompetencia: "  Gestión ", nombreFactor: "Análisis " },
    null,
  );

  assert.equal(resultado.factores.length, 1);
  assert.equal(resultado.factores[0].id, 10);
  assert.equal(resultado.factores[0].codigo, "FAC-01");
  assert.equal(resultado.factores[0].competenciaId, 3);
  assert.equal(resultado.factores[0].nombre, "Capacidad de análisis");
  assert.equal(resultado.factores[0].nombreCompetencia, "Gestión de proyectos");
  assert.equal(resultado.factores[0].orden, 1);
  assert.equal(resultado.siguienteCursor, null);
});

test("listarFactores calcula siguienteCursor cuando recibe una página completa de 50 elementos", async () => {
  const paginaCompleta = Array.from({ length: 50 }, (_, i) => ({
    id: i + 1,
    codigo: `FAC-${i + 1}`,
    nombreFactor: `Factor ${i + 1}`,
    nombreCompetencia: "Competencia A",
    orden: i + 1,
    descripcion: "Desc",
  }));

  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, `${API_BASE_URL}/factores?offset=50&cantidad=50`);
    return Response.json(paginaCompleta);
  });

  const resultado = await listarFactores({ nombreCompetencia: "", nombreFactor: "" }, 50);
  assert.equal(resultado.factores.length, 50);
  assert.equal(resultado.siguienteCursor, 100);
});

test("obtenerFactorPorId y listarFactoresPorCompetencia consultan los endpoints GET correspondientes", async () => {
  const urlsLlamadas = [];
  mock.method(globalThis, "fetch", async (url) => {
    urlsLlamadas.push(url);
    if (String(url).endsWith("/factores/7")) {
      return Response.json({
        id: 7,
        competenciaId: 2,
        codigo: "FAC-07",
        nombre: "Comunicación asertiva",
        orden: 3,
        descripcion: "Claridad al transmitir ideas",
      });
    }
    return Response.json([
      {
        id: 7,
        competenciaId: 2,
        codigo: "FAC-07",
        nombre: "Comunicación asertiva",
        orden: 3,
        descripcion: "Claridad al transmitir ideas",
      },
    ]);
  });

  const detalle = await obtenerFactorPorId(7);
  assert.equal(detalle.id, 7);
  assert.equal(detalle.competenciaId, 2);

  const porCompetencia = await listarFactoresPorCompetencia(2);
  assert.equal(porCompetencia.length, 1);
  assert.equal(porCompetencia[0].codigo, "FAC-07");

  assert.deepEqual(urlsLlamadas, [
    `${API_BASE_URL}/factores/7`,
    `${API_BASE_URL}/factores/competencia/2`,
  ]);
});

test("crearFactor envía POST /factores y devuelve el factor creado", async () => {
  mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, `${API_BASE_URL}/factores`);
    assert.equal(options.method, "POST");
    assert.equal(options.headers.get("Content-Type"), "application/json");

    const body = JSON.parse(options.body);
    assert.deepEqual(body, {
      competenciaId: 1,
      codigo: "FAC-10",
      nombre: "Planificación",
      orden: 1,
      descripcion: "Organización de tareas",
    });

    return Response.json({ id: 15, ...body }, { status: 201 });
  });

  const creado = await crearFactor({
    competenciaId: 1,
    codigo: "FAC-10",
    nombre: "Planificación",
    orden: 1,
    descripcion: "Organización de tareas",
  });

  assert.equal(creado.id, 15);
  assert.equal(creado.codigo, "FAC-10");
});

test("modificarFactor envía PUT /factores/{id} y eliminarFactor/eliminarFactores envían DELETE /factores/{id}", async () => {
  const peticiones = [];
  mock.method(globalThis, "fetch", async (url, options) => {
    peticiones.push({ url, method: options.method });
    if (options.method === "PUT") {
      const body = JSON.parse(options.body);
      return Response.json({ id: 15, ...body });
    }
    return new Response(null, { status: 204 });
  });

  const actualizado = await modificarFactor(15, {
    competenciaId: 2,
    codigo: "FAC-10",
    nombre: "Planificación estratégica",
    orden: 2,
    descripcion: "Nueva descripción",
  });

  assert.equal(actualizado.id, 15);
  assert.equal(actualizado.nombre, "Planificación estratégica");

  await eliminarFactor(15);
  await eliminarFactores([20, 21]);

  assert.deepEqual(peticiones, [
    { url: `${API_BASE_URL}/factores/15`, method: "PUT" },
    { url: `${API_BASE_URL}/factores/15`, method: "DELETE" },
    { url: `${API_BASE_URL}/factores/20`, method: "DELETE" },
    { url: `${API_BASE_URL}/factores/21`, method: "DELETE" },
  ]);
});

test("crearFactor propaga ApiError 409 ante duplicidad de código, nombre u orden", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 409,
        error: "Recurso Duplicado",
        message: "El código del factor ya se encuentra registrado.",
        path: "/api/factores",
      },
      { status: 409 },
    );
  });

  await assert.rejects(
    crearFactor({
      competenciaId: 1,
      codigo: "FAC-01",
      nombre: "Capacidad",
      orden: 1,
      descripcion: "Desc",
    }),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.match(error.message, /El código del factor ya se encuentra registrado/);
      return true;
    },
  );
});
