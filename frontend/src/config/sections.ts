export const sections = [
  {
    id: "candidatos",
    title: "Candidatos",
    group: "Personas",
    icon: "users",
    description: "Centralizá los candidatos que participan en tus procesos de evaluación.",
    upcoming: "Importar candidatos, consultar sus datos y prepararlos para una evaluación.",
    related: ["evaluaciones", "cuestionarios"],
  },
  {
    id: "puestos",
    title: "Puestos",
    group: "Catálogos",
    icon: "briefcase",
    description: "Definí los perfiles y las competencias que requiere cada puesto.",
    upcoming: "Buscar puestos y definir sus competencias, empresas y ponderaciones.",
    related: ["competencias", "evaluaciones"],
  },
  {
    id: "competencias",
    title: "Competencias",
    group: "Catálogos",
    icon: "target",
    description: "Organizá las capacidades que se valoran en cada perfil profesional.",
    upcoming: "Consultar y registrar competencias con su código, nombre y descripción.",
    related: ["factores", "puestos"],
  },
  {
    id: "factores",
    title: "Factores",
    group: "Catálogos",
    icon: "layers",
    description: "Desglosá cada competencia en los factores que se van a evaluar.",
    upcoming: "Consultar y registrar factores asociados a una competencia.",
    related: ["competencias", "preguntas"],
  },
  {
    id: "preguntas",
    title: "Preguntas",
    group: "Catálogos",
    icon: "message",
    description: "Construí el banco de preguntas para tus cuestionarios de evaluación.",
    upcoming: "Buscar, crear, modificar y dar de baja preguntas por competencia y factor.",
    related: ["factores", "opciones-respuesta"],
  },
  {
    id: "opciones-respuesta",
    title: "Opciones de respuesta",
    group: "Catálogos",
    icon: "list",
    description: "Definí las alternativas de respuesta y sus puntajes de evaluación.",
    upcoming: "Consultar y crear conjuntos de respuestas con sus ítems y orden de presentación.",
    related: ["preguntas", "competencias"],
  },
  {
    id: "evaluaciones",
    title: "Evaluaciones",
    group: "Evaluación",
    icon: "clipboard",
    description: "Acompañá cada proceso, desde la selección del puesto hasta sus resultados.",
    upcoming: "Seleccionar un puesto y sus candidatos para generar una evaluación.",
    related: ["candidatos", "orden-merito"],
  },
  {
    id: "cuestionarios",
    title: "Cuestionarios",
    group: "Evaluación",
    icon: "file",
    description: "Accedé a los cuestionarios que forman parte de una evaluación.",
    upcoming: "Consultar los cuestionarios asignados y completar sus bloques de preguntas.",
    related: ["evaluaciones", "preguntas"],
  },
  {
    id: "orden-merito",
    title: "Orden de mérito",
    group: "Evaluación",
    icon: "award",
    description: "Reuní los resultados para comparar candidatos según el perfil del puesto.",
    upcoming: "Consultar el orden de mérito y los puntajes de una evaluación por puesto.",
    related: ["evaluaciones", "puestos"],
  },
] as const;

export type Section = (typeof sections)[number];
export type SectionId = Section["id"];

export function getSection(id: string) {
  return sections.find((section) => section.id === id);
}
