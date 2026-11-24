/**
 * Ventana deslizante de 1 minuto en memoria.
 * Limitación documentada: en Vercel cada instancia tiene su propia memoria, así que el límite es
 * aproximado (suficiente para proteger la cuota gratuita de Gemini en un proyecto académico).
 */
const windows = new Map<string, number[]>();
const WINDOW_MS = 60_000;

export function allow(key: string, max: number, now = Date.now()): boolean {
  const marks = (windows.get(key) ?? []).filter((t) => now - t < WINDOW_MS);
  if (marks.length >= max) { windows.set(key, marks); return false; }
  marks.push(now);
  windows.set(key, marks);
  if (windows.size > 5_000) windows.clear(); // evita crecer sin límite
  return true;
}
