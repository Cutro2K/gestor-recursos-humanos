"use client";

import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { Avatar, Button, DropdownButton, Modal, Stack } from "react-ui-lib";
import { useSession } from "@/components/auth/session-provider";
import { Icon } from "@/components/ui/icon";

export function UserProfile() {
  const { session } = useSession();
  // proxy.ts garantiza la sesión dentro del sistema; null solo mientras hidrata.
  return session ? <SignedInMenu name={session.nombre} rol={session.rol} /> : null;
}

function SignedInMenu({ name, rol }: { name: string; rol: string }) {
  const router = useRouter();
  const { signOut } = useSession();
  const [accountOpen, setAccountOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const triggerRef = useRef<HTMLButtonElement>(null);

  return (
    <div className="relative flex size-9 shrink-0">
      <DropdownButton
        hideArrow
        keepSource
        direction="column"
        position="bottom_left"
        triggerLabel={
          <Avatar
            aria-hidden="true"
            name={name}
            alt=""
            size="sm"
            className="size-9 text-xs [--ui-border:color-mix(in_srgb,var(--ui-surface)_75%,white)]"
          />
        }
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
            key: "account",
            children: "Mi cuenta",
            leadingIcon: (
              <span
                aria-hidden="true"
                className="inline-block size-5 shrink-0 bg-current mask-[url(/icons/user.png)] mask-contain mask-center mask-no-repeat"
              />
            ),
            onClick: () => setAccountOpen(true),
          },
          {
            key: "logout",
            children: "Cerrar sesión",
            variant: "destructive",
            leadingIcon: <Icon name="logout" strokeWidth={1.5} />,
            onClick: () => setLogoutOpen(true),
          },
        ]}
      />
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
          <p className="text-sm text-[var(--ui-muted)]">Rol: {rol.toLowerCase()}</p>
        </Stack>
      </Modal>
      <Modal
        open={logoutOpen}
        onOpenChange={setLogoutOpen}
        originRef={triggerRef}
        title="¿Cerrar sesión?"
        description="Vas a volver a la pantalla de inicio de sesión."
        size="sm"
        actions={
          <>
            <Button variant="secondary" onClick={() => setLogoutOpen(false)}>
              Cancelar
            </Button>
            <Button
              variant="destructive"
              leadingIcon={<Icon name="logout" strokeWidth={1.5} />}
              onClick={() => {
                signOut();
                router.replace("/login");
              }}
            >
              Cerrar sesión
            </Button>
          </>
        }
      >
        <p className="text-sm text-[var(--ui-muted)]">Sesión actual: {name}</p>
      </Modal>
    </div>
  );
}
