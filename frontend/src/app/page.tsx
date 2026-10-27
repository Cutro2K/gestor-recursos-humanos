import Link from "next/link";
import { Stack, Surface } from "react-ui-lib";
import { getSection, sections } from "@/config/sections";
import { PageHeader } from "@/components/ui/page-header";
import { Icon } from "@/components/ui/icon";
import { SectionLink } from "@/components/ui/section-link";
import { TextLink } from "@/components/ui/text-link";

const steps = [
  {
    id: "puestos",
    title: "Definí el perfil",
    text: "Organizá puestos, competencias y los factores que querés evaluar.",
    label: "Explorar puestos",
  },
  {
    id: "candidatos",
    title: "Reuní el talento",
    text: "Prepará los candidatos que van a participar de cada proceso.",
    label: "Ver candidatos",
  },
  {
    id: "evaluaciones",
    title: "Acompañá el proceso",
    text: "Conectá candidatos y puestos, y consultá los resultados de la evaluación.",
    label: "Ir a evaluaciones",
  },
] as const;

const process = [
  { icon: "briefcase", title: "Un perfil claro", text: "Puestos y competencias" },
  { icon: "users", title: "El potencial de cada persona", text: "Candidatos y cuestionarios" },
  { icon: "award", title: "Resultados para decidir", text: "Evaluaciones y orden de mérito" },
] as const;

export default function Home() {
  return (
    <Stack gap="lg">
      <PageHeader
        eyebrow="Tu espacio de trabajo"
        title="Cada persona, una oportunidad."
        description="Organizá los perfiles y acompañá cada etapa de la evaluación de talento."
      />
      <Surface bordered padding="lg">
        <div className="grid items-center gap-6 md:grid-cols-2">
          <Stack gap="lg" align="start">
            <p className="text-sm text-[var(--ui-muted)]">Una mirada integral del talento</p>
            <h2 className="text-2xl font-semibold">De las competencias a las oportunidades.</h2>
            <p className="text-sm leading-relaxed text-[var(--ui-muted)]">
              Todo lo que necesitás para organizar tus procesos, conectado en un mismo lugar.
            </p>
            <TextLink href="/evaluaciones">
              Explorar evaluaciones
              <Icon name="arrow" />
            </TextLink>
          </Stack>
          <Stack gap="md" aria-label="Etapas del proceso">
            {process.map((step) => (
              <Surface key={step.icon} bordered padding="sm">
                <Stack direction="row" align="center" gap="md">
                  <Icon name={step.icon} className="shrink-0 text-[var(--ui-accent)]" />
                  <div>
                    <strong className="text-sm font-semibold">{step.title}</strong>
                    <p className="text-sm text-[var(--ui-muted)]">{step.text}</p>
                  </div>
                </Stack>
              </Surface>
            ))}
          </Stack>
        </div>
      </Surface>
      <section aria-labelledby="start-title">
        <Stack gap="md">
          <Stack direction="row" justify="between" gap="sm" wrap>
            <h2 id="start-title" className="text-lg font-semibold">
              ¿Por dónde empezamos?
            </h2>
            <span className="text-sm text-[var(--ui-muted)]">Un recorrido, tres etapas</span>
          </Stack>
          <div className="grid gap-4 md:grid-cols-3">
            {steps.map((step, index) => (
              <Link href={`/${step.id}`} className="block h-full" key={step.id}>
                <Surface bordered className="h-full">
                  <Stack gap="lg" className="h-full">
                    <Stack direction="row" justify="between" align="center">
                      <Icon name={getSection(step.id)!.icon} className="text-[var(--ui-accent)]" />
                      <span className="text-xs text-[var(--ui-muted)]">0{index + 1}</span>
                    </Stack>
                    <h3 className="text-lg font-semibold">{step.title}</h3>
                    <p className="flex-1 text-sm leading-relaxed text-[var(--ui-muted)]">
                      {step.text}
                    </p>
                    <Stack
                      direction="row"
                      justify="between"
                      align="center"
                      className="text-sm text-[var(--ui-accent)]"
                    >
                      <span>{step.label}</span>
                      <Icon name="arrow" />
                    </Stack>
                  </Stack>
                </Surface>
              </Link>
            ))}
          </div>
        </Stack>
      </section>
      <section aria-labelledby="catalog-title">
        <Stack gap="md">
          <Stack direction="row" justify="between" gap="sm" wrap>
            <h2 id="catalog-title" className="text-lg font-semibold">
              Las bases de cada evaluación
            </h2>
            <span className="text-sm text-[var(--ui-muted)]">Catálogos y contenidos</span>
          </Stack>
          <div className="grid gap-4 md:grid-cols-2">
            {sections
              .filter((section) =>
                ["competencias", "factores", "preguntas", "opciones-respuesta"].includes(
                  section.id,
                ),
              )
              .map((section) => (
                <SectionLink key={section.id} section={section} />
              ))}
          </div>
        </Stack>
      </section>
    </Stack>
  );
}
