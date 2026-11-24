import { GoalInput, PhraseOutput } from "../lib/schemas.js";
import { generate, cleanText } from "../lib/ai.js";
import { json, readBody, rateLimit } from "../lib/http.js";
import { localGoalPhrase } from "../lib/fallback.js";

/**
 * POST /api/meta — explica en 1 frase el cambio de meta. La meta NO se calcula aquí:
 * la calcula el reloj con reglas deterministas; la IA solo la explica.
 */
export async function POST(req: Request) {
  const input = await readBody(req, GoalInput);
  if (input instanceof Response) return input;
  const limited = rateLimit(req, input.dispositivo);
  if (limited) return limited;
  const { dispositivo: _, ...data } = input;
  const result = await generate(
    input.idioma,
    "La meta diaria de pasos se ajustó según la última semana (tendencia: sube, baja o igual). Explica el cambio en UNA frase de máximo 9 palabras (debe caber en 2 líneas de un reloj redondo), en primera persona, sin cifras.",
    data,
    PhraseOutput,
    12,
  );
  return json(result ? { frase: cleanText(result.frase), origen: "ia" } : { frase: localGoalPhrase(input.idioma, input.tendencia), origen: "local" });
}
