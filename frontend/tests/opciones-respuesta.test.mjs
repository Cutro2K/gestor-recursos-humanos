import assert from "node:assert/strict";
import { register } from "node:module";
import { afterEach, mock, test } from "node:test";

register(new URL("./ts-resolver.mjs", import.meta.url));

const { API_BASE_URL, ApiError } = await import("../src/service/api.ts");
const {
  consultarUsoOpcionRespuesta,
  crearOpcionRespuesta,
  eliminarOpcionRespuesta,
  listarOpcionesRespuesta,
  modificarOpcionRespuesta,
  obtenerOpcionRespuestaPorId,
} = await import("../src/service/opciones-respuesta.ts");

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
  assert.equal(resultado.siguienteOffset, null);
});

test("listarOpcionesRespuesta omite nombre cuando está vacío y calcula siguienteOffset en páginas completas de 50", async () => {
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
  assert.equal(resultado.siguienteOffset, 100);
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

test("crearOpcionRespuesta envía POST /opciones-respuesta con cuerpo JSON y retorna la opción creada", async () => {
  const payload = {
    nombre: "ESCALA ACUERDO",
    descripcion: "ESCALA DE TRES NIVELES DE ACUERDO",
    items: [
      { orden: 1, descripcion: "EN DESACUERDO" },
      { orden: 2, descripcion: "NEUTRAL" },
      { orden: 3, descripcion: "DE ACUERDO" },
    ],
  };

  mock.method(globalThis, "fetch", async (url, init) => {
    assert.equal(url, `${API_BASE_URL}/opciones-respuesta`);
    assert.equal(init.method, "POST");
    assert.deepEqual(JSON.parse(init.body), payload);
    return Response.json(
      {
        id: 15,
        ...payload,
        items: payload.items.map((item, idx) => ({ id: idx + 100, ...item })),
      },
      { status: 201 },
    );
  });

  const creada = await crearOpcionRespuesta(payload);

  assert.equal(creada.id, 15);
  assert.equal(creada.nombre, "ESCALA ACUERDO");
  assert.equal(creada.items.length, 3);
  assert.equal(creada.items[0].id, 100);
});

test("crearOpcionRespuesta propaga ApiError 409 con erroresValidacion cuando el nombre activo ya existe", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 409,
        error: "Recurso Duplicado",
        message: "Ya existe una opción de respuesta activa con el nombre 'VERDADERO_FALSO'.",
        path: "/api/opciones-respuesta",
        erroresValidacion: {
          nombre: "Ya existe una opción de respuesta activa con el nombre 'VERDADERO_FALSO'.",
        },
      },
      { status: 409 },
    );
  });

  await assert.rejects(
    crearOpcionRespuesta({
      nombre: "VERDADERO_FALSO",
      descripcion: "OPCIONES DICOTÓMICAS",
      items: [
        { orden: 1, descripcion: "VERDADERO" },
        { orden: 2, descripcion: "FALSO" },
      ],
    }),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.equal(
        error.details.erroresValidacion.nombre,
        "Ya existe una opción de respuesta activa con el nombre 'VERDADERO_FALSO'.",
      );
      return true;
    },
  );
});

test("modificarOpcionRespuesta envía PUT /opciones-respuesta/{id} con descripcion e items existentes y devuelve la opción actualizada", async () => {
  const payload = {
    descripcion: "ESCALA VALORATIVA ACTUALIZADA",
    items: [
      { id: 10, orden: 2, descripcion: "REGULAR" },
      { id: 11, orden: 1, descripcion: "MALO" },
    ],
  };

  mock.method(globalThis, "fetch", async (url, init) => {
    assert.equal(url, `${API_BASE_URL}/opciones-respuesta/2`);
    assert.equal(init.method, "PUT");
    assert.deepEqual(JSON.parse(init.body), payload);
    return Response.json({
      id: 2,
      nombre: "ESCALA_DESEMPENO",
      descripcion: payload.descripcion,
      items: [
        { id: 11, orden: 1, descripcion: "MALO" },
        { id: 10, orden: 2, descripcion: "REGULAR" },
      ],
    });
  });

  const actualizada = await modificarOpcionRespuesta(2, payload);

  assert.equal(actualizada.id, 2);
  assert.equal(actualizada.descripcion, "ESCALA VALORATIVA ACTUALIZADA");
  assert.equal(actualizada.items.length, 2);
  assert.equal(actualizada.items[0].id, 11);
  assert.equal(actualizada.items[0].orden, 1);
});

test("modificarOpcionRespuesta propaga ApiError 400 con erroresValidacion cuando la validación del backend falla", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 400,
        error: "Datos de Entrada Inválidos",
        message: "Error de validación en los campos enviados.",
        path: "/api/opciones-respuesta/2",
        erroresValidacion: {
          items: "El orden de visualización debe ser consecutivo comenzando en 1.",
          "items[0].descripcion": "La descripción del ítem es obligatoria.",
        },
      },
      { status: 400 },
    );
  });

  await assert.rejects(
    modificarOpcionRespuesta(2, {
      descripcion: "DESC",
      items: [
        { id: 10, orden: 1, descripcion: "" },
        { id: 11, orden: 3, descripcion: "REGULAR" },
      ],
    }),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 400);
      assert.equal(
        error.details.erroresValidacion["items[0].descripcion"],
        "La descripción del ítem es obligatoria.",
      );
      return true;
    },
  );
});

test("eliminarOpcionRespuesta envía DELETE /opciones-respuesta/{id} y resuelve ante respuesta 204", async () => {
  const peticiones = [];
  mock.method(globalThis, "fetch", async (url, init) => {
    peticiones.push({ url, method: init.method });
    return new Response(null, { status: 204 });
  });

  await eliminarOpcionRespuesta(5);

  assert.deepEqual(peticiones, [
    { url: `${API_BASE_URL}/opciones-respuesta/5`, method: "DELETE" },
  ]);
});

test("eliminarOpcionRespuesta propaga ApiError 409 cuando la opción está en uso por preguntas activas", async () => {
  mock.method(globalThis, "fetch", async () => {
    return Response.json(
      {
        status: 409,
        error: "Recurso en Uso",
        message:
          "La opción de respuesta ESCALA_DESEMPENO está siendo usada en la base de datos y no puede eliminarse.",
        path: "/api/opciones-respuesta/2",
      },
      { status: 409 },
    );
  });

  await assert.rejects(
    eliminarOpcionRespuesta(2),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.equal(
        error.message,
        "La opción de respuesta ESCALA_DESEMPENO está siendo usada en la base de datos y no puede eliminarse.",
      );
      return true;
    },
  );
});



test("consultarUsoOpcionRespuesta consulta GET /opciones-respuesta/{id}/uso y devuelve enUso", async () => {
  const urls = [];
  mock.method(globalThis, "fetch", async (url) => {
    urls.push(url);
    return Response.json({ enUso: urls.length === 1 });
  });

  assert.deepEqual(await consultarUsoOpcionRespuesta(7), { enUso: true });
  assert.deepEqual(await consultarUsoOpcionRespuesta(8), { enUso: false });
  assert.deepEqual(urls, [
    `${API_BASE_URL}/opciones-respuesta/7/uso`,
    `${API_BASE_URL}/opciones-respuesta/8/uso`,
  ]);
});

test("consultarUsoOpcionRespuesta rechaza una respuesta sin el campo enUso", async () => {
  mock.method(globalThis, "fetch", async () => Response.json({}));

  await assert.rejects(consultarUsoOpcionRespuesta(7), /No pudimos verificar/);
});
