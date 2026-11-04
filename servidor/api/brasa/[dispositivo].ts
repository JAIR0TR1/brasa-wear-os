import { Dispositivo, EstadoBrasa } from "../../lib/esquemas.js";
import { guardarEstado, leerEstado } from "../../lib/almacen.js";
import { error, json, leer, limitar } from "../../lib/http.js";

/** Id del último segmento de la ruta /api/brasa/{dispositivo}. */
function idDe(req: Request): string | Response {
  const id = decodeURIComponent(new URL(req.url).pathname.split("/").filter(Boolean).pop() ?? "");
  const r = Dispositivo.safeParse(id);
  return r.success ? r.data : error(400, "dispositivo_invalido", "Id de dispositivo no válido.");
}

/** GET /api/brasa/{dispositivo} — nivel, xp, etapa y racha guardados. */
export async function GET(req: Request) {
  const id = idDe(req);
  if (id instanceof Response) return id;
  const lim = limitar(req, id);
  if (lim) return lim;
  const e = await leerEstado(id);
  return e ? json(e) : error(404, "no_encontrado", "Sin estado guardado para este reloj.");
}

/** PUT /api/brasa/{dispositivo} — guarda nivel, xp, etapa y racha (nada de salud). */
export async function PUT(req: Request) {
  const id = idDe(req);
  if (id instanceof Response) return id;
  const lim = limitar(req, id);
  if (lim) return lim;
  const d = await leer(req, EstadoBrasa);
  if (d instanceof Response) return d;
  return json(await guardarEstado(id, d));
}
