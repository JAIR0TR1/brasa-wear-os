import { z } from "zod";

/**
 * Contratos del servidor. Todo son valores cerrados (enums y enteros acotados):
 * - nunca texto libre del usuario (la IA no recibe nada que pueda inyectar instrucciones),
 * - nunca pasos ni pulso exactos (solo niveles agregados).
 * Los objetos son estrictos: un campo de más se rechaza.
 */
export const Language = z.enum(["es", "en"]);
export const Level = z.enum(["bajo", "medio", "alto"]);
export const Emotion = z.enum(["apagada", "inquieta", "tranquila", "feliz", "euforica"]);
export const Stage = z.enum(["chispa", "llama", "fogata", "leyenda"]);
export const Situation = z.enum(["saludo", "inactividad", "tension", "post_respiracion", "meta_cumplida", "subida_nivel"]);
/** Id aleatorio por reloj (UUID). No identifica a la persona. */
export const DeviceId = z.string().regex(/^[0-9a-f-]{32,40}$/i, "dispositivo inválido");

export const MessageInput = z.strictObject({
  dispositivo: DeviceId,
  idioma: Language,
  situacion: Situation,
  actividad: Level,
  emocion: Emotion,
  etapa: Stage,
});

export const GoalInput = z.strictObject({
  dispositivo: DeviceId,
  idioma: Language,
  /** Cómo cambió la meta que ya calculó el reloj con sus reglas. */
  tendencia: z.enum(["sube", "baja", "igual"]),
  /** Días de la semana con la meta cumplida, agregados. */
  constancia: Level,
  actividadMedia: Level,
});

export const SummaryInput = z.strictObject({
  dispositivo: DeviceId,
  idioma: Language,
  actividad: Level,
  metaCumplida: z.boolean(),
  respiraciones: z.int().min(0).max(20),
  tension: z.enum(["ninguna", "alguna", "frecuente"]),
  emocionFinal: Emotion,
});

export const BrasaState = z.strictObject({
  nivel: z.int().min(1).max(500),
  xp: z.int().min(0).max(10_000_000),
  etapa: Stage,
  racha: z.int().min(0).max(3650),
});

/** Salidas de la IA (también se validan: si no cumplen, se usa el respaldo local). */
export const PhraseOutput = z.strictObject({ frase: z.string().min(2).max(140) });
export const SummaryOutput = z.strictObject({ frases: z.array(z.string().min(2).max(160)).length(2) });

export type TMessage = z.infer<typeof MessageInput>;
export type TGoal = z.infer<typeof GoalInput>;
export type TSummary = z.infer<typeof SummaryInput>;
export type TState = z.infer<typeof BrasaState>;
