import { MessageInput, PhraseOutput } from "../lib/schemas.js";
import { generate, cleanText } from "../lib/ai.js";
import { json, readBody, rateLimit } from "../lib/http.js";
import { localPhrase } from "../lib/fallback.js";

const SITUATION_PROMPT: Record<string, string> = {
  saludo: "Saluda al empezar el día.",
  inactividad: "La persona lleva rato sin moverse: anímala con suavidad a caminar un poco.",
  tension: "Hay señales de tensión estando en reposo: propón respirar un minuto contigo, con calma.",
  post_respiracion: "Acaba de terminar un minuto de respiración guiada: reconócelo con calidez.",
  meta_cumplida: "Acaba de cumplir su meta de pasos del día: celébralo con alegría.",
  subida_nivel: "Brasa acaba de subir de nivel: celébralo.",
};

/** POST /api/mensaje — una frase corta (≤ 15 palabras de contrato; se piden ≤ 9 para que quepa en 2 líneas del reloj). */
export async function POST(req: Request) {
  const input = await readBody(req, MessageInput);
  if (input instanceof Response) return input;
  const limited = rateLimit(req, input.dispositivo);
  if (limited) return limited;
  const { dispositivo: _, ...data } = input; // el id no se envía a la IA
  const result = await generate(
    input.idioma,
    `${SITUATION_PROMPT[input.situacion]} Escribe UNA frase de máximo 9 palabras (debe caber en 2 líneas de un reloj redondo).`,
    data,
    PhraseOutput,
    12,
  );
  return json(result ? { frase: cleanText(result.frase), origen: "ia" } : { frase: localPhrase(input.idioma, input.situacion), origen: "local" });
}
