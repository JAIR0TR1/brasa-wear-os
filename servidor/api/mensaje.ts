import { MensajeEntrada, SalidaFrase } from "../lib/esquemas.js";
import { generar, limpiar } from "../lib/ia.js";
import { json, leer, limitar } from "../lib/http.js";
import { fraseLocal } from "../lib/respaldo.js";

export const config = { maxDuration: 10 };

const SITUACION: Record<string, string> = {
  saludo: "Saluda al empezar el día.",
  inactividad: "La persona lleva rato sin moverse: anímala con suavidad a caminar un poco.",
  tension: "Hay señales de tensión estando en reposo: propón respirar un minuto contigo, con calma.",
  post_respiracion: "Acaba de terminar un minuto de respiración guiada: reconócelo con calidez.",
  meta_cumplida: "Acaba de cumplir su meta de pasos del día: celébralo con alegría.",
  subida_nivel: "Brasa acaba de subir de nivel: celébralo.",
};

/** POST /api/mensaje — una frase de máx. 15 palabras con la personalidad de Brasa. */
export async function POST(req: Request) {
  const d = await leer(req, MensajeEntrada);
  if (d instanceof Response) return d;
  const lim = limitar(req, d.dispositivo);
  if (lim) return lim;
  const { dispositivo: _, ...datos } = d; // el id no se envía a la IA
  const ia = await generar(
    d.idioma,
    `${SITUACION[d.situacion]} Escribe UNA frase de máximo 15 palabras.`,
    datos,
    SalidaFrase,
    15,
  );
  return json(ia ? { frase: limpiar(ia.frase), origen: "ia" } : { frase: fraseLocal(d.idioma, d.situacion), origen: "local" });
}
