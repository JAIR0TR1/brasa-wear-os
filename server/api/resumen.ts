import { SummaryInput, SummaryOutput } from "../lib/schemas.js";
import { generate, cleanText } from "../lib/ai.js";
import { json, readBody, rateLimit } from "../lib/http.js";
import { localSummary } from "../lib/fallback.js";

/** POST /api/resumen — resumen del día en 2 frases a partir de agregados. */
export async function POST(req: Request) {
  const input = await readBody(req, SummaryInput);
  if (input instanceof Response) return input;
  const limited = rateLimit(req, input.dispositivo);
  if (limited) return limited;
  const { dispositivo: _, ...data } = input;
  const result = await generate(
    input.idioma,
    "Resume el día de la persona en EXACTAMENTE 2 frases cortas (máximo 8 palabras cada una): la primera sobre lo que pasó, la segunda una invitación amable para mañana.",
    data,
    SummaryOutput,
    11,
  );
  return json(result ? { frases: result.frases.map(cleanText), origen: "ia" } : { frases: localSummary(input.idioma), origen: "local" });
}
