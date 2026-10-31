import assert from "node:assert/strict";
import { afterEach, mock, test } from "node:test";
import { API_BASE_URL, ApiError } from "../src/service/api.ts";
import { crearCompetencia, listarCompetencias } from "../src/service/competencias.ts";

afterEach(() => mock.restoreAll());

test("listarCompetencias envía query params correctos y devuelve el listado", async () => {
  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(
      url,
      `${API_BASE_URL}/competencias?nombre=Liderazgo&codigo=COMP-01&tipo=ACTITUDINAL`,
    );
    return Response.json([
      {
        id: 1,
        codigo: "COMP-01",
        nombre: "Liderazgo",
        tipo: "ACTITUDINAL",
        descripcion: "Habilidad de conducción.",
        eliminada: false,
      },
    ]);
  });

  const resultado = await listarCompetencias({
    nombre: "Liderazgo",
    codigo: "COMP-01",
    tipo: "ACTITUDINAL",
  });

  assert.equal(resultado.length, 1);
  assert.equal(resultado[0].codigo, "COMP-01");
  assert.equal(resultado[0].tipo, "ACTITUDINAL");
});

test("crearCompetencia serializa el payload en POST y retorna la competencia creada con status 201", async () => {
  mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, `${API_BASE_URL}/competencias`);
    assert.equal(options.method, "POST");
    assert.equal(options.headers.get("Content-Type"), "application/json");

    const body = JSON.parse(options.body);
    assert.equal(body.codigo, "COMP-05");
    assert.equal(body.nombre, "Arquitectura Cloud");
    assert.equal(body.tipo, "TECNICA");
    assert.equal(body.descripcion, "Diseño de sistemas distribuidos.");

    return Response.json(
      {
        id: 5,
        codigo: "COMP-05",
        nombre: "Arquitectura Cloud",
        tipo: "TECNICA",
        descripcion: "Diseño de sistemas distribuidos.",
        eliminada: false,
      },
      { status: 201 },
    );
  });

  const creada = await crearCompetencia({
    codigo: "COMP-05",
    nombre: "Arquitectura Cloud",
    tipo: "TECNICA",
    descripcion: "Diseño de sistemas distribuidos.",
  });

  assert.equal(creada.id, 5);
  assert.equal(creada.codigo, "COMP-05");
});

test("crearCompetencia propaga ApiError con status 409 cuando el código o nombre ya existen", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 409,
        error: "Recurso Duplicado",
        message: "Ya existe una competencia registrada con el código 'COMP-01'.",
        path: "/api/competencias",
      },
      { status: 409, headers: { "Content-Type": "application/json" } },
    );
  });

  await assert.rejects(
    crearCompetencia({
      codigo: "COMP-01",
      nombre: "Nombre duplicado",
      tipo: "TECNICA",
    }),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.match(error.message, /Ya existe una competencia registrada con el código 'COMP-01'/);
      return true;
    },
  );
});
