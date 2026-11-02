import type { Variants } from "motion/react";

/**
 * Entrada del login: cada bloque aparece desde abajo con un fade.
 * `stagger` va en el contenedor y escalona a sus hijos con `fadeUp`;
 * los contenedores anidados también usan `stagger` y siguen la secuencia.
 */
export const stagger: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.07, delayChildren: 0.05 } },
};

export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] } },
};

/** fadeUp para un bloque que además escalona su propio contenido. */
export const fadeUpGroup: Variants = {
  hidden: fadeUp.hidden,
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1], staggerChildren: 0.07 },
  },
};
