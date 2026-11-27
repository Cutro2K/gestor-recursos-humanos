import assert from "node:assert/strict";
import { test } from "node:test";
import {
  LIMITES_OPCION_RESPUESTA,
  mapearErroresBackendAlta,
  mapearErroresBackendModificacion,
  sinErroresAltaOpcionRespuesta,
  sinErroresModificacionOpcionRespuesta,
  sugerirSiguienteOrden,
  validarAltaOpcionRespuesta,
  validarModificacionOpcionRespuesta,
} from "../src/components/opciones-respuesta/opciones-respuesta-validation.ts";

test("acepta una opción de respuesta válida con órdenes consecutivos 1..N", () => {
  const errores = validarAltaOpcionRespuesta({
    nombre: "ESCALA FRECUENCIA",
    descripcion: "TRES NIVELES DE FRECUENCIA",
    items: [
      { orden: "2", descripcion: "A VECES" },
      { orden: "1", descripcion: "NUNCA" },
      { orden: "3", descripcion: "SIEMPRE" },
    ],
  });

  assert.ok(sinErroresAltaOpcionRespuesta(errores));
});

test("detecta campos en blanco en nombre, descripción e ítems de opción", () => {
  const errores = validarAltaOpcionRespuesta({
    nombre: "   ",
    descripcion: "",
    items: [
      { orden: " ", descripcion: "   " },
      { orden: "2", descripcion: "NO" },
    ],
  });

  assert.equal(sinErroresAltaOpcionRespuesta(errores), false);
  assert.ok(errores.nombre);
  assert.ok(errores.descripcion);
  assert.ok(errores.filas[0].orden);
  assert.ok(errores.filas[0].descripcion);
  assert.equal(errores.filas[1].orden, undefined);
  assert.equal(errores.filas[1].descripcion, undefined);
});

test("aplica los topes máximos de caracteres: 50 en nombre, 1000 en descripción y 150 en ítem", () => {
  assert.equal(LIMITES_OPCION_RESPUESTA.nombre, 50);
  assert.equal(LIMITES_OPCION_RESPUESTA.descripcion, 1000);
  assert.equal(LIMITES_OPCION_RESPUESTA.descripcionItem, 150);

  const exactos = validarAltaOpcionRespuesta({
    nombre: "N".repeat(50),
    descripcion: "D".repeat(1000),
    items: [
      { orden: "1", descripcion: "A".repeat(150) },
      { orden: "2", descripcion: "B".repeat(150) },
    ],
  });
  assert.ok(sinErroresAltaOpcionRespuesta(exactos));

  const excedidos = validarAltaOpcionRespuesta({
    nombre: "N".repeat(51),
    descripcion: "D".repeat(1001),
    items: [
      { orden: "1", descripcion: "A".repeat(151) },
      { orden: "2", descripcion: "B" },
    ],
  });
  assert.ok(excedidos.nombre);
  assert.ok(excedidos.descripcion);
  assert.ok(excedidos.filas[0].descripcion);
});

test("exige al menos dos ítems de opción", () => {
  const errores = validarAltaOpcionRespuesta({
    nombre: "UNICO",
    descripcion: "SOLO UN ITEM",
    items: [{ orden: "1", descripcion: "SI" }],
  });

  assert.equal(sinErroresAltaOpcionRespuesta(errores), false);
  assert.ok(errores.items);
});

test("rechaza órdenes no positivos o no enteros y secuencias que no sean consecutivas 1..N", () => {
  const cero = validarAltaOpcionRespuesta({
    nombre: "ORDEN CERO",
    descripcion: "CON CERO",
    items: [
      { orden: "0", descripcion: "UNO" },
      { orden: "1", descripcion: "DOS" },
    ],
  });
  assert.ok(cero.filas[0].orden);

  const negativo = validarAltaOpcionRespuesta({
    nombre: "ORDEN NEGATIVO",
    descripcion: "CON NEGATIVO",
    items: [
      { orden: "-1", descripcion: "UNO" },
      { orden: "1", descripcion: "DOS" },
    ],
  });
  assert.ok(negativo.filas[0].orden);

  const empiezaEnDos = validarAltaOpcionRespuesta({
    nombre: "EMPIEZA EN DOS",
    descripcion: "ARRANCA EN 2",
    items: [
      { orden: "2", descripcion: "UNO" },
      { orden: "3", descripcion: "DOS" },
    ],
  });
  assert.ok(empiezaEnDos.items);

  const conHueco = validarAltaOpcionRespuesta({
    nombre: "CON HUECO",
    descripcion: "SALTA EL 2",
    items: [
      { orden: "1", descripcion: "UNO" },
      { orden: "3", descripcion: "TRES" },
    ],
  });
  assert.ok(conHueco.items);

  const ordenDuplicado = validarAltaOpcionRespuesta({
    nombre: "ORDEN REPETIDO",
    descripcion: "DOS VECES 1",
    items: [
      { orden: "1", descripcion: "UNO" },
      { orden: "1", descripcion: "DOS" },
    ],
  });
  assert.ok(ordenDuplicado.items);
  assert.ok(ordenDuplicado.filas[1].orden);
});

test("rechaza descripciones de ítems duplicadas sin distinguir mayúsculas/minúsculas ni espacios extremos", () => {
  const errores = validarAltaOpcionRespuesta({
    nombre: "VERDADERO FALSO",
    descripcion: "OPCION REPETIDA",
    items: [
      { orden: "1", descripcion: "Verdadero" },
      { orden: "2", descripcion: "  VERDADERO  " },
    ],
  });

  assert.equal(sinErroresAltaOpcionRespuesta(errores), false);
  assert.equal(errores.filas[0].descripcion, undefined);
  assert.ok(errores.filas[1].descripcion);
});

test("sugerirSiguienteOrden propone el próximo entero positivo disponible", () => {
  assert.equal(sugerirSiguienteOrden(["1", "2"]), "3");
  assert.equal(sugerirSiguienteOrden(["1", "4"]), "5");
  assert.equal(sugerirSiguienteOrden([]), "1");
});

test("mapearErroresBackendAlta traduce erroresValidacion de campos, lista y filas indexadas", () => {
  const mapeado = mapearErroresBackendAlta(
    {
      nombre: "Nombre duplicado.",
      items: "Orden no consecutivo.",
      "items[0].orden": "Orden inválido.",
      "items[1].descripcion": "Descripción obligatoria.",
    },
    2,
  );

  assert.ok(mapeado);
  assert.equal(mapeado.nombre, "Nombre duplicado.");
  assert.equal(mapeado.items, "Orden no consecutivo.");
  assert.equal(mapeado.filas[0].orden, "Orden inválido.");
  assert.equal(mapeado.filas[1].descripcion, "Descripción obligatoria.");
});

test("validarModificacionOpcionRespuesta acepta modificación válida sin exigir nombre editable y valida descripción, secuencia 1..N y duplicados", () => {
  const valida = validarModificacionOpcionRespuesta({
    descripcion: "ESCALA VALORATIVA ACTUALIZADA",
    items: [
      { orden: "2", descripcion: "REGULAR" },
      { orden: "1", descripcion: "MALO" },
    ],
  });
  assert.ok(sinErroresModificacionOpcionRespuesta(valida));

  const invalida = validarModificacionOpcionRespuesta({
    descripcion: "   ",
    items: [
      { orden: "1", descripcion: "Igual" },
      { orden: "3", descripcion: "  IGUAL " },
    ],
  });
  assert.equal(sinErroresModificacionOpcionRespuesta(invalida), false);
  assert.ok(invalida.descripcion);
  assert.ok(invalida.items);
  assert.ok(invalida.filas[1].descripcion);
});

test("mapearErroresBackendModificacion traduce erroresValidacion de descripcion, items y parámetros por fila incluyendo id", () => {
  const mapeado = mapearErroresBackendModificacion(
    {
      descripcion: "La descripción es obligatoria.",
      items: "El orden de visualización debe ser consecutivo comenzando en 1.",
      "items[0].id": "El identificador del ítem es obligatorio.",
      "items[0].orden": "El orden debe ser un entero positivo.",
      "items[1].descripcion": "La descripción del ítem es obligatoria.",
    },
    2,
  );

  assert.ok(mapeado);
  assert.equal(mapeado.descripcion, "La descripción es obligatoria.");
  assert.equal(
    mapeado.items,
    "El orden de visualización debe ser consecutivo comenzando en 1.",
  );
  assert.equal(mapeado.filas[0].id, "El identificador del ítem es obligatorio.");
  assert.equal(mapeado.filas[0].orden, "El orden debe ser un entero positivo.");
  assert.equal(mapeado.filas[1].descripcion, "La descripción del ítem es obligatoria.");
});
