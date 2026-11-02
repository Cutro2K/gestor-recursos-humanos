"use client";

import { motion } from "motion/react";
import { useState } from "react";
import type { FormEvent } from "react";
import { Button, Stack, Tabs, TextField, useToast } from "react-ui-lib";
import { Icon } from "@/components/ui/icon";
import { loginCandidato, loginConsultor, tiposDocumento } from "@/service/auth";
import type { TipoDocumento } from "@/service/auth";
import { ApiError } from "@/service/api";
import { fadeUp, stagger } from "./fade-up";
import {
  LIMITES,
  normalizarDocumento,
  sinErrores,
  validarCandidato,
  validarConsultor,
} from "./login-validation";
import type { ErroresCandidato, ErroresConsultor } from "./login-validation";
import { useSession } from "./session-provider";
import styles from "./login-form.module.css";

type Rol = "consultor" | "candidato";
type Errores = ErroresConsultor & ErroresCandidato;

/** Formulario del CU-01. Sus bloques se suman a la entrada escalonada de la pantalla. */
export function LoginForm({ onSuccess }: { onSuccess: () => void }) {
  const { signIn } = useSession();
  const { toast } = useToast();

  const [rol, setRol] = useState<Rol>("consultor");
  const [usuario, setUsuario] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [tipoDocumento, setTipoDocumento] = useState<TipoDocumento>("DNI");
  const [nroDocumento, setNroDocumento] = useState("");
  const [clave, setClave] = useState("");
  const [errores, setErrores] = useState<Errores>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Cambia un campo y borra su error: el mensaje desaparece en cuanto se corrige.
  function campo(nombre: keyof Errores, setValor: (valor: string) => void) {
    return (valor: string) => {
      setValor(valor);
      setErrores((actuales) => ({ ...actuales, [nombre]: undefined }));
    };
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    // Se valida antes de llamar a la API; con errores no se envía nada.
    const encontrados =
      rol === "consultor"
        ? validarConsultor(usuario, contrasena)
        : validarCandidato(tipoDocumento, nroDocumento, clave);
    setErrores(encontrados);
    if (!sinErrores(encontrados)) return;

    const documento = normalizarDocumento(tipoDocumento, nroDocumento);
    setLoading(true);
    try {
      const sesion =
        rol === "consultor"
          ? await loginConsultor(usuario.trim(), contrasena)
          : await loginCandidato(tipoDocumento, documento, clave);
      const nombre = rol === "consultor" ? usuario.trim() : `${tipoDocumento} ${documento}`;
      signIn({ ...sesion, nombre });
      toast({ title: "Sesión iniciada", description: `Bienvenido, ${nombre}.` });
      onSuccess();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo iniciar sesión.");
      setLoading(false);
    }
  }

  const consultorFields = (
    <Stack gap="md" className="pt-4">
      <TextField
        label="Usuario"
        name="usuario"
        autoComplete="username"
        required
        maxLength={LIMITES.usuario}
        error={errores.usuario}
        value={usuario}
        onChange={campo("usuario", setUsuario)}
      />
      <TextField
        label="Contraseña"
        name="contrasena"
        type="password"
        autoComplete="current-password"
        required
        maxLength={LIMITES.contrasena}
        error={errores.contrasena}
        value={contrasena}
        onChange={campo("contrasena", setContrasena)}
      />
    </Stack>
  );

  const candidatoFields = (
    <Stack gap="md" className="pt-4">
      {/* Tipo y número en una fila: el grupo queda casi tan alto como el de consultor. */}
      <div className="grid grid-cols-[6.5rem_1fr] items-start gap-3">
        {/* La biblioteca no tiene Select: se usan sus clases de campo para que coincida. */}
        <fieldset className={`ui-text-field-outline ${styles.outline}`}>
          <legend className="ui-text-field-legend">
            <label htmlFor="tipoDocumento">Tipo</label>
          </legend>
          <div className="flex items-center">
            <select
              id="tipoDocumento"
              name="tipoDocumento"
              value={tipoDocumento}
              onChange={(event) => {
                setTipoDocumento(event.target.value as TipoDocumento);
                setErrores((actuales) => ({ ...actuales, nroDocumento: undefined }));
              }}
              className={styles.select}
            >
              {tiposDocumento.map((tipo) => (
                <option key={tipo}>{tipo}</option>
              ))}
            </select>
            <Icon name="chevron" width={16} height={16} className={styles.chevron} />
          </div>
        </fieldset>
        <TextField
          label="Número de documento"
          name="nroDocumento"
          inputMode={tipoDocumento === "PP" ? "text" : "numeric"}
          required
          maxLength={LIMITES.nroDocumento}
          error={errores.nroDocumento}
          value={nroDocumento}
          onChange={campo("nroDocumento", setNroDocumento)}
        />
      </div>
      <TextField
        label="Clave del cuestionario"
        name="clave"
        type="password"
        helpText="8 letras o números."
        required
        maxLength={LIMITES.clave}
        error={errores.clave}
        value={clave}
        onChange={campo("clave", setClave)}
      />
    </Stack>
  );

  // Ambos grupos ocupan la misma celda: la altura no cambia al cambiar de pestaña,
  // así el modal no se mueve. El inactivo queda invisible y fuera de la validación.
  const fields = (
    <div className="grid">
      <fieldset disabled={rol !== "consultor"} className={fieldsetClass(rol === "consultor")}>
        {consultorFields}
      </fieldset>
      <fieldset disabled={rol !== "candidato"} className={fieldsetClass(rol === "candidato")}>
        {candidatoFields}
      </fieldset>
    </div>
  );

  return (
    // noValidate: se muestran los mensajes propios en lugar de los globos del navegador.
    <motion.form variants={stagger} noValidate onSubmit={handleSubmit}>
      <Stack gap="md">
        <motion.div variants={fadeUp}>
          <Tabs
            fullWidth
            aria-label="Tipo de usuario"
            value={rol}
            onValueChange={(value) => {
              setRol(value as Rol);
              setError(null);
              setErrores({});
            }}
            tabs={[
              { value: "consultor", label: "Consultor", content: fields },
              { value: "candidato", label: "Candidato", content: fields },
            ]}
          />
        </motion.div>
        {error && (
          <p role="alert" className="text-sm text-[var(--ui-danger)]">
            {error}
          </p>
        )}
        <motion.div variants={fadeUp}>
          <Button
            type="submit"
            loading={loading}
            leadingIcon={<Icon name="login" />}
            className="w-full"
          >
            Ingresar
          </Button>
        </motion.div>
      </Stack>
    </motion.form>
  );
}

function fieldsetClass(active: boolean) {
  return `[grid-area:1/1] min-w-0 ${active ? "" : "invisible"}`;
}
