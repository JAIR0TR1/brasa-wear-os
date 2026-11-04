import { ResumenEntrada, SalidaResumen } from "../lib/esquemas.js";
import { generar, limpiar } from "../lib/ia.js";
import { json, leer, limitar } from "../lib/http.js";
import { resumenLocal } from "../lib/respaldo.js";

export const config = { maxDuration: 10 };

/** POST /api/resumen — resumen del día en 2 frases a partir de agregados. */
export async function POST(req: Request) {
  const d = await leer(req, ResumenEntrada);
  if (d instanceof Response) return d;
  const lim = limitar(req, d.dispositivo);
  if (lim) return lim;
  const { dispositivo: _, ...datos } = d;
  const ia = await generar(
    d.idioma,
    "Resume el día de la persona en EXACTAMENTE 2 frases cortas (máximo 14 palabras cada una): la primera sobre lo que pasó, la segunda una invitación amable para mañana.",
    datos,
    SalidaResumen,
    14,
  );
  return json(ia ? { frases: ia.frases.map(limpiar), origen: "ia" } : { frases: resumenLocal(d.idioma), origen: "local" });
}
