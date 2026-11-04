/**
 * Ventana deslizante de 1 minuto en memoria.
 * Limitación documentada: en Vercel cada instancia tiene su propia memoria, así que el límite es
 * aproximado (suficiente para proteger la cuota gratuita de Gemini en un proyecto académico).
 */
const ventanas = new Map<string, number[]>();
const VENTANA_MS = 60_000;

export function permitir(clave: string, maximo: number, ahora = Date.now()): boolean {
  const marcas = (ventanas.get(clave) ?? []).filter((t) => ahora - t < VENTANA_MS);
  if (marcas.length >= maximo) { ventanas.set(clave, marcas); return false; }
  marcas.push(ahora);
  ventanas.set(clave, marcas);
  if (ventanas.size > 5_000) ventanas.clear(); // evita crecer sin límite
  return true;
}
