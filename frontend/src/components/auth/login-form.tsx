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
import { useSession } from "./session-provider";
import styles from "./login-form.module.css";

type Rol = "consultor" | "candidato";

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
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const sesion =
        rol === "consultor"
          ? await loginConsultor(usuario, contrasena)
          : await loginCandidato(tipoDocumento, nroDocumento, clave);
      const nombre = rol === "consultor" ? usuario : `${tipoDocumento} ${nroDocumento}`;
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
        value={usuario}
        onChange={setUsuario}
      />
      <TextField
        label="Contraseña"
        name="contrasena"
        type="password"
        autoComplete="current-password"
        required
        value={contrasena}
        onChange={setContrasena}
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
              onChange={(event) => setTipoDocumento(event.target.value as TipoDocumento)}
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
          inputMode="numeric"
          required
          value={nroDocumento}
          onChange={setNroDocumento}
        />
      </div>
      <TextField
        label="Clave del cuestionario"
        name="clave"
        type="password"
        helpText="8 caracteres."
        minLength={8}
        maxLength={8}
        required
        value={clave}
        onChange={setClave}
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
    <motion.form variants={stagger} onSubmit={handleSubmit}>
      <Stack gap="md">
        <motion.div variants={fadeUp}>
          <Tabs
            fullWidth
            aria-label="Tipo de usuario"
            value={rol}
            onValueChange={(value) => {
              setRol(value as Rol);
              setError(null);
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
