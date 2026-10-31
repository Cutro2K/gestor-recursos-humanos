import assert from "node:assert/strict";
import { afterEach, mock, test } from "node:test";
import { API_BASE_URL, ApiError, apiRequest } from "../src/service/api.ts";

afterEach(() => mock.restoreAll());

test("GET joins paths, keeps custom headers and reads JSON", async () => {
  mock.method(globalThis, "fetch", async (url, options) => {
    assert.equal(url, `${API_BASE_URL}/competencias?nombre=equipo`);
    assert.equal(options.cache, "no-store");
    assert.equal(options.headers.get("Accept"), "application/json");
    assert.equal(options.headers.get("Authorization"), "Bearer ejemplo");
    return Response.json([{ id: 1, nombre: "Trabajo en equipo" }]);
  });
  const result = await apiRequest("/competencias?nombre=equipo", {
    headers: { Authorization: "Bearer ejemplo" },
  });
  assert.deepEqual(result, [{ id: 1, nombre: "Trabajo en equipo" }]);
});

test("POST serializes JSON and preserves method", async () => {
  mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.method, "POST");
    assert.equal(options.headers.get("Content-Type"), "application/json");
    assert.equal(options.body, '{"nombre":"Comunicación"}');
    return Response.json({ id: 2 }, { status: 201 });
  });
  assert.deepEqual(
    await apiRequest("competencias", { method: "POST", json: { nombre: "Comunicación" } }),
    { id: 2 },
  );
});

test("FormData is sent without forcing a multipart Content-Type", async () => {
  const form = new FormData();
  form.set("archivo", new Blob(["nombre\nEjemplo"], { type: "text/csv" }), "candidatos.csv");
  mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.body, form);
    assert.equal(options.headers.has("Content-Type"), false);
    return new Response(null, { status: 204 });
  });
  assert.equal(await apiRequest("candidatos/importar", { method: "POST", body: form }), undefined);
});

test("204 responses do not attempt JSON parsing", async () => {
  mock.method(globalThis, "fetch", async () => new Response(null, { status: 204 }));
  assert.equal(await apiRequest("preguntas/1", { method: "DELETE" }), undefined);
});

test("HTTP errors retain status and validation details", async () => {
  const details = { message: "El código ya existe.", fields: { codigo: "Duplicado" } };
  mock.method(globalThis, "fetch", async () => Response.json(details, { status: 409 }));
  await assert.rejects(apiRequest("puestos"), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 409);
    assert.equal(error.message, details.message);
    assert.deepEqual(error.details, details);
    return true;
  });
});

test("Problem Details errors use the detail field", async () => {
  mock.method(globalThis, "fetch", async () =>
    Response.json({ detail: "Falta el nombre." }, { status: 400 }),
  );
  await assert.rejects(apiRequest("puestos"), { message: "Falta el nombre.", status: 400 });
});

test("HTML errors produce feedback instead of displaying server markup", async () => {
  mock.method(
    globalThis,
    "fetch",
    async () =>
      new Response("<h1>Bad Gateway</h1>", {
        status: 502,
        headers: { "Content-Type": "text/html" },
      }),
  );
  await assert.rejects(
    apiRequest("puestos"),
    (error) => error.status === 502 && !error.message.includes("<h1>"),
  );
});

test("network failures produce an ApiError with status zero", async () => {
  mock.method(globalThis, "fetch", async () => {
    throw new TypeError("fetch failed");
  });
  await assert.rejects(
    apiRequest("puestos"),
    (error) => error instanceof ApiError && error.status === 0,
  );
});

test("cancellation preserves the original abort error and signal", async () => {
  const controller = new AbortController();
  const abort = new DOMException("Cancelado", "AbortError");
  controller.abort();
  mock.method(globalThis, "fetch", async (_url, options) => {
    assert.equal(options.signal, controller.signal);
    throw abort;
  });
  await assert.rejects(
    apiRequest("puestos", { signal: controller.signal }),
    (error) => error === abort,
  );
});

test("invalid JSON is reported as an invalid response", async () => {
  mock.method(
    globalThis,
    "fetch",
    async () => new Response("{", { headers: { "Content-Type": "application/json" } }),
  );
  await assert.rejects(
    apiRequest("puestos"),
    (error) => error instanceof ApiError && error.message.includes("JSON inválida"),
  );
});
