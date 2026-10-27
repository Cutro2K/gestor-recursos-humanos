"use client";

import { useRef, useState } from "react";
import { Avatar, Button, DropdownButton, Modal, Stack, useToast } from "react-ui-lib";
import { Icon } from "@/components/ui/icon";

type UserProfileProps = { name: string };

export function UserProfile({ name }: UserProfileProps) {
  const [accountOpen, setAccountOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const { toast } = useToast();

  return (
    <div className="relative flex size-9 shrink-0">
      <DropdownButton
        hideArrow
        direction="column"
        position="bottom_left"
        triggerLabel={null}
        triggerProps={{
          ref: triggerRef,
          variant: "secondary",
          size: "sm",
          className: "size-9 min-h-0 rounded-full border-0 p-0",
          "aria-label": `Abrir menú de usuario: ${name}`,
          title: name,
        }}
        buttons={[
          {
            key: "login",
            children: "Iniciar sesión",
            leadingIcon: <Icon name="login" strokeWidth={1.5} />,
            onClick: () =>
              toast({
                title: "Inicio de sesión no disponible",
                description: "La autenticación todavía no está conectada.",
              }),
          },
          {
            key: "account",
            children: "Mi cuenta",
            onClick: () => setAccountOpen(true),
          },
          {
            key: "logout",
            children: "Cerrar sesión",
            variant: "destructive",
            leadingIcon: <Icon name="logout" strokeWidth={1.5} />,
            onClick: () =>
              toast({
                title: "No hay una sesión activa",
                description: "El inicio de sesión todavía no está conectado.",
              }),
          },
        ]}
      />
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 z-10">
        <Avatar
          name={name}
          alt=""
          size="sm"
          className="size-9 text-xs [--ui-border:color-mix(in_srgb,var(--ui-surface)_75%,white)]"
        />
      </div>
      <Modal
        open={accountOpen}
        onOpenChange={setAccountOpen}
        originRef={triggerRef}
        title="Mi cuenta"
        size="sm"
        actions={<Button onClick={() => setAccountOpen(false)}>Cerrar</Button>}
      >
        <Stack gap="sm">
          <p className="font-medium">{name}</p>
          <p className="text-sm text-[var(--ui-muted)]">
            Los datos de tu cuenta estarán disponibles cuando se conecte el inicio de sesión.
          </p>
        </Stack>
      </Modal>
    </div>
  );
}
