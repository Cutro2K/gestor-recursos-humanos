"use client";

import { useState } from "react";
import type { FormEvent } from "react";
import { Button, Modal, Stack, Surface, TextField, useToast } from "react-ui-lib";
import { Feedback } from "@/components/ui/feedback";
import { PageHeader } from "@/components/ui/page-header";
import { TextLink } from "@/components/ui/text-link";

export default function UIPage() {
  const [name, setName] = useState("");
  const [error, setError] = useState("");
  const [open, setOpen] = useState(false);
  const { success } = useToast();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!name.trim()) {
      setError("Ingresá un nombre para continuar.");
      return;
    }
    setError("");
    success("Prueba completada", `Hola, ${name.trim()}. No se guardaron datos.`);
  }

  return (
    <Stack gap="lg" className="mx-auto max-w-3xl">
      <TextLink href="/" className="self-start">
        Volver al inicio
      </TextLink>
      <PageHeader
        title="Componentes de interfaz"
        description="Prueba de ReactUILib. Los controles usan la apariencia elegida y la preferencia de movimiento del sistema. Este formulario no envía ni guarda datos."
      />
      <section aria-labelledby="form-title">
        <Surface bordered>
          <Stack gap="lg">
            <h2 id="form-title" className="text-xl font-semibold">
              Formulario y notificaciones
            </h2>
            <form onSubmit={handleSubmit} noValidate>
              <Stack gap="lg">
                <TextField
                  label="Nombre de prueba"
                  value={name}
                  onChange={(value) => {
                    setName(value);
                    setError("");
                  }}
                  error={error}
                  helpText="Usá un nombre ficticio para probar el formulario."
                  required
                />
                <Stack direction="row" gap="sm" wrap>
                  <Button type="submit">Probar notificación</Button>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      setName("");
                      setError("");
                    }}
                  >
                    Limpiar
                  </Button>
                </Stack>
              </Stack>
            </form>
          </Stack>
        </Surface>
      </section>
      <Feedback title="Controles listos para reutilizar" variant="success">
        Los botones, campos y mensajes mantienen el mismo estilo en todas las secciones.
      </Feedback>
      <section aria-labelledby="modal-title">
        <Stack gap="md" align="start">
          <h2 id="modal-title" className="text-xl font-semibold">
            Ventana modal
          </h2>
          <Modal
            open={open}
            onOpenChange={setOpen}
            title="Vista de prueba"
            description="Probá el foco, la animación y el cierre con la tecla Escape."
            trigger={<Button variant="secondary">Abrir modal</Button>}
            actions={<Button onClick={() => setOpen(false)}>Entendido</Button>}
          >
            <p>Esta ventana usa los componentes y estilos de ReactUILib.</p>
          </Modal>
        </Stack>
      </section>
    </Stack>
  );
}
