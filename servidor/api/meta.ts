import { MetaEntrada, SalidaFrase } from "../lib/esquemas.js";
import { generar, limpiar } from "../lib/ia.js";
import { json, leer, limitar } from "../lib/http.js";
import { metaLocal } from "../lib/respaldo.js";

/**
 * POST /api/meta — explica en 1 frase el cambio de meta. La meta NO se calcula aquí:
 * la calcula el reloj con reglas deterministas; la IA solo la explica.
 */
export async function POST(req: Request) {
  const d = await leer(req, MetaEntrada);
  if (d instanceof Response) return d;
  const lim = limitar(req, d.dispositivo);
  if (lim) return lim;
  const { dispositivo: _, ...datos } = d;
  const ia = await generar(
    d.idioma,
    "La meta diaria de pasos se ajustó según la última semana (tendencia: sube, baja o igual). Explica el cambio en UNA frase de máximo 9 palabras (debe caber en 2 líneas de un reloj redondo), en primera persona, sin cifras.",
    datos,
    SalidaFrase,
    12,
  );
  return json(ia ? { frase: limpiar(ia.frase), origen: "ia" } : { frase: metaLocal(d.idioma, d.tendencia), origen: "local" });
}
