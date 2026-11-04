import type { TEstado } from "./esquemas.js";

/**
 * Estado de Brasa por dispositivo (nivel, XP, etapa, racha). Nada de salud.
 * Limitación documentada: almacenamiento en memoria (se pierde al reiniciar la instancia de Vercel).
 * Siguiente paso previsto: Upstash Redis desde el Marketplace de Vercel (plan gratuito).
 */
const memoria = new Map<string, TEstado & { actualizado: string }>([
  // Dato de ejemplo para la demo.
  ["00000000-0000-4000-8000-000000000000", { nivel: 7, xp: 940, etapa: "llama", racha: 4, actualizado: new Date(0).toISOString() }],
]);

export const tipoAlmacen = "memoria";

export async function leerEstado(id: string) {
  return memoria.get(id) ?? null;
}

export async function guardarEstado(id: string, e: TEstado) {
  const v = { ...e, actualizado: new Date().toISOString() };
  memoria.set(id, v);
  if (memoria.size > 10_000) memoria.delete(memoria.keys().next().value!);
  return v;
}
