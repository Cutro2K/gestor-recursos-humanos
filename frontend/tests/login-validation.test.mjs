import assert from "node:assert/strict";
import { test } from "node:test";
import {
  LIMITES,
  normalizarDocumento,
  sinErrores,
  validarCandidato,
  validarConsultor,
} from "../src/components/auth/login-validation.ts";

test("consultor: acepta usuario y contraseña válidos", () => {
  assert.ok(sinErrores(validarConsultor("ADMIN", "admin123")));
});

test("consultor: exige ambos campos y no cuenta los espacios como usuario", () => {
  const errores = validarConsultor("   ", "");
  assert.ok(errores.usuario);
  assert.ok(errores.contrasena);
});

test("consultor: respeta el máximo de consultor.username (30)", () => {
  assert.ok(sinErrores(validarConsultor("a".repeat(LIMITES.usuario), "x")));
  assert.ok(validarConsultor("a".repeat(LIMITES.usuario + 1), "x").usuario);
});

test("consultor: limita el largo de la contraseña", () => {
  assert.ok(validarConsultor("ADMIN", "x".repeat(LIMITES.contrasena + 1)).contrasena);
});

test("candidato: acepta los documentos de los datos de ejemplo", () => {
  assert.ok(sinErrores(validarCandidato("DNI", "40123456", "A1B2C3D4")));
  assert.ok(sinErrores(validarCandidato("DNI", "00123456", "A1B2C3D4")));
  assert.ok(sinErrores(validarCandidato("PP", "AA998877", "Z9Y8X7W6")));
});

test("candidato: DNI, LE y LC llevan 7 u 8 números; se aceptan puntos y espacios", () => {
  assert.ok(sinErrores(validarCandidato("DNI", "40.123.456", "A1B2C3D4")));
  assert.ok(sinErrores(validarCandidato("LE", "1234567", "A1B2C3D4")));
  assert.ok(validarCandidato("DNI", "123456", "A1B2C3D4").nroDocumento);
  assert.ok(validarCandidato("LC", "123456789", "A1B2C3D4").nroDocumento);
  assert.ok(validarCandidato("DNI", "40A23456", "A1B2C3D4").nroDocumento);
});

test("candidato: el pasaporte acepta letras y números hasta 20", () => {
  assert.ok(sinErrores(validarCandidato("PP", "ab001234", "A1B2C3D4")));
  assert.ok(validarCandidato("PP", "AB-001234", "A1B2C3D4").nroDocumento);
  assert.ok(validarCandidato("PP", "A".repeat(LIMITES.nroDocumento + 1), "A1B2C3D4").nroDocumento);
});

test("candidato: la clave tiene exactamente 8 letras o números", () => {
  assert.ok(validarCandidato("DNI", "40123456", "").clave);
  assert.ok(validarCandidato("DNI", "40123456", "A1B2C3D").clave);
  assert.ok(validarCandidato("DNI", "40123456", "A1B2C3D45").clave);
  assert.ok(validarCandidato("DNI", "40123456", "A1B2-3D4").clave);
});

test("normalizarDocumento quita puntos y espacios del DNI y pasa el pasaporte a mayúsculas", () => {
  assert.equal(normalizarDocumento("DNI", " 40.123 456 "), "40123456");
  assert.equal(normalizarDocumento("PP", " aa998877 "), "AA998877");
});
