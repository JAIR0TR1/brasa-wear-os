import type { TState } from "./schemas.js";

/**
 * Estado de Brasa por dispositivo (nivel, XP, etapa, racha). Nada de salud.
 * Limitación documentada: almacenamiento en memoria (se pierde al reiniciar la instancia de Vercel).
 * Siguiente paso previsto: Upstash Redis desde el Marketplace de Vercel (plan gratuito).
 */
const memory = new Map<string, TState & { actualizado: string }>();

/** Valor que /api/salud publica en el campo "almacen". */
export const storeType = "memoria";

export async function readState(id: string) {
  return memory.get(id) ?? null;
}

export async function saveState(id: string, state: TState) {
  const saved = { ...state, actualizado: new Date().toISOString() };
  memory.set(id, saved);
  if (memory.size > 10_000) memory.delete(memory.keys().next().value!);
  return saved;
}
