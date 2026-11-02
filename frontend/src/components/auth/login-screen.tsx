"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { MotionConfig, motion } from "motion/react";
import { Icon } from "@/components/ui/icon";
import { TextLink } from "@/components/ui/text-link";
import { fadeUp, fadeUpGroup, stagger } from "./fade-up";
import { LoginForm } from "./login-form";
import { guestSession, useSession } from "./session-provider";
import styles from "./login-screen.module.css";

/** Pantalla inicial: proxy.ts redirige acá cualquier ruta mientras no haya sesión. */
export function LoginScreen() {
  const router = useRouter();
  const { signIn } = useSession();

  return (
    // reducedMotion="user": sin animaciones si el sistema pide reducir movimiento.
    <MotionConfig reducedMotion="user">
      {/* data-app-shell: globals.css lo usa para aplicar el color-scheme del tema. */}
      <motion.div
        data-app-shell
        className={`${styles.page} bg-[var(--ui-bg)] text-[var(--ui-text)]`}
        variants={stagger}
        initial="hidden"
        animate="visible"
      >
        {/* Columna izquierda: marca, formulario y acceso de invitado. */}
        <motion.section className={styles.formColumn} variants={stagger}>
          <motion.div variants={fadeUp} className="flex items-center gap-2">
            <Image src="/brand/gestor-rrhh.svg" alt="" width={28} height={28} />
            <strong className="text-sm">GESTOR DE RRHH</strong>
          </motion.div>

          <motion.div variants={stagger} className="mx-auto w-full max-w-sm">
            <motion.div
              variants={fadeUp}
              className="mb-10 flex flex-col items-center gap-3 text-center"
            >
              <span className={styles.brandTile}>
                <Image src="/brand/gestor-rrhh.svg" alt="" width={32} height={32} />
              </span>
              <h1 className="text-2xl font-semibold">Iniciá sesión en tu cuenta</h1>
              <p className="text-sm text-[var(--ui-muted)]">
                Elegí tu tipo de usuario e ingresá tus datos.
              </p>
            </motion.div>
            <LoginForm onSuccess={() => router.replace("/")} />
          </motion.div>

          <motion.div
            variants={fadeUp}
            className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 text-sm text-[var(--ui-muted)]"
          >
            <span>¿Todavía no tenés cuenta?</span>
            {/* Sin prefetch: antes del clic no hay cookie y el proxy redirigiría a /login. */}
            <TextLink
              href="/"
              replace
              prefetch={false}
              className="whitespace-nowrap"
              onClick={() => signIn(guestSession)}
            >
              Entrar como invitado
              <Icon name="chevron" width={16} height={16} />
            </TextLink>
          </motion.div>
        </motion.section>

        {/* Columna derecha (solo pantallas anchas): mensaje y vista previa del sistema. */}
        <motion.aside className={styles.showcase} variants={fadeUpGroup} aria-hidden="true">
          <motion.h2 variants={fadeUp} className="text-3xl font-semibold">
            Cada persona, una oportunidad.
          </motion.h2>
          <motion.p variants={fadeUp} className="mt-3 max-w-md text-white/85">
            Organizá puestos, competencias y candidatos, y acompañá cada evaluación desde un
            solo espacio de trabajo.
          </motion.p>
          <motion.div variants={fadeUp} className={styles.preview}>
            {/* Capturas reales del inicio; el CSS muestra la del tema activo. */}
            <div className={styles.tilt}>
              {/* eslint-disable-next-line @next/next/no-img-element -- imágenes decorativas */}
              <img className={styles.light} src="/login/vista-previa-claro.webp" alt="" />
              {/* eslint-disable-next-line @next/next/no-img-element -- imágenes decorativas */}
              <img className={styles.dark} src="/login/vista-previa-oscuro.webp" alt="" />
            </div>
          </motion.div>
        </motion.aside>
      </motion.div>
    </MotionConfig>
  );
}
