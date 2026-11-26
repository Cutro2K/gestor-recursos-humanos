import assert from "node:assert/strict";
import { register } from "node:module";
import { afterEach, mock, test } from "node:test";

register(new URL("./ts-resolver.mjs", import.meta.url));

const { API_BASE_URL, ApiError } = await import("../src/service/api.ts");
const { listarOpcionesRespuesta, obtenerOpcionRespuestaPorId } = await import(
  "../src/service/opciones-respuesta.ts"
);

afterEach(() => mock.restoreAll());

test("listarOpcionesRespuesta envía offset, cantidad y filtro nombre recortado cuando se informa", async () => {
  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(
      url,
      `${API_BASE_URL}/opciones-respuesta?offset=0&cantidad=50&nombre=Escala+100%25`,
    );
    return Response.json([
      {
        id: 1,
        nombre: "ESCALA 100%",
        descripcion: "Escala porcentual completa.",
      },
    ]);
  });

  const resultado = await listarOpcionesRespuesta({ nombre: "  Escala 100%  " }, null);

  assert.equal(resultado.opciones.length, 1);
  assert.deepEqual(resultado.opciones[0], {
    id: 1,
    nombre: "ESCALA 100%",
    descripcion: "Escala porcentual completa.",
  });
  assert.equal(resultado.siguienteCursor, null);
});

test("listarOpcionesRespuesta omite nombre cuando está vacío y calcula siguienteCursor en páginas completas de 50", async () => {
  const paginaCompleta = Array.from({ length: 50 }, (_, i) => ({
    id: i + 1,
    nombre: `OPCION_${i + 1}`,
    descripcion: `Descripción ${i + 1}`,
  }));

  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, `${API_BASE_URL}/opciones-respuesta?offset=50&cantidad=50`);
    return Response.json(paginaCompleta);
  });

  const resultado = await listarOpcionesRespuesta({ nombre: "   " }, 50);

  assert.equal(resultado.opciones.length, 50);
  assert.equal(resultado.siguienteCursor, 100);
});

test("obtenerOpcionRespuestaPorId consulta GET /opciones-respuesta/{id} y devuelve los ítems de opción ordenados", async () => {
  mock.method(globalThis, "fetch", async (url) => {
    assert.equal(url, `${API_BASE_URL}/opciones-respuesta/2`);
    return Response.json({
      id: 2,
      nombre: "ESCALA_DESEMPENO",
      descripcion: "Escala valorativa de cinco niveles.",
      items: [
        { id: 10, orden: 1, descripcion: "Malo" },
        { id: 11, orden: 2, descripcion: "Regular" },
      ],
    });
  });

  const detalle = await obtenerOpcionRespuestaPorId(2);

  assert.equal(detalle.id, 2);
  assert.equal(detalle.nombre, "ESCALA_DESEMPENO");
  assert.equal(detalle.items.length, 2);
  assert.equal(detalle.items[0].orden, 1);
  assert.equal(detalle.items[0].descripcion, "Malo");
  assert.equal(detalle.items[1].orden, 2);
});

test("obtenerOpcionRespuestaPorId propaga ApiError 404 cuando la opción de respuesta no existe o está eliminada", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 404,
        error: "Recurso No Encontrado",
        message: "No se encontró la opción de respuesta con ID 99.",
        path: "/api/opciones-respuesta/99",
      },
      { status: 404 },
    );
  });

  await assert.rejects(
    obtenerOpcionRespuestaPorId(99),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 404);
      assert.match(error.message, /No se encontró la opción de respuesta/);
      return true;
    },
  );
});
