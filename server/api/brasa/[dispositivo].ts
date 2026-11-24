import { DeviceId, BrasaState } from "../../lib/schemas.js";
import { saveState, readState } from "../../lib/store.js";
import { error, json, readBody, rateLimit } from "../../lib/http.js";

/** Id del último segmento de la ruta /api/brasa/{dispositivo}. */
function idOf(req: Request): string | Response {
  const id = decodeURIComponent(new URL(req.url).pathname.split("/").filter(Boolean).pop() ?? "");
  const result = DeviceId.safeParse(id);
  return result.success ? result.data : error(400, "dispositivo_invalido", "Id de dispositivo no válido.");
}

/** GET /api/brasa/{dispositivo} — nivel, xp, etapa y racha guardados. */
export async function GET(req: Request) {
  const id = idOf(req);
  if (id instanceof Response) return id;
  const limited = rateLimit(req, id);
  if (limited) return limited;
  const state = await readState(id);
  return state ? json(state) : error(404, "no_encontrado", "Sin estado guardado para este reloj.");
}

/** PUT /api/brasa/{dispositivo} — guarda nivel, xp, etapa y racha (nada de salud). */
export async function PUT(req: Request) {
  const id = idOf(req);
  if (id instanceof Response) return id;
  const limited = rateLimit(req, id);
  if (limited) return limited;
  const input = await readBody(req, BrasaState);
  if (input instanceof Response) return input;
  return json(await saveState(id, input));
}
