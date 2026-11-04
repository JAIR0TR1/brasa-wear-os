import { z } from "zod";

/**
 * Contratos del servidor. Todo son valores cerrados (enums y enteros acotados):
 * - nunca texto libre del usuario (la IA no recibe nada que pueda inyectar instrucciones),
 * - nunca pasos ni pulso exactos (solo niveles agregados).
 * Los objetos son estrictos: un campo de más se rechaza.
 */
export const Idioma = z.enum(["es", "en"]);
export const Nivel = z.enum(["bajo", "medio", "alto"]);
export const Emocion = z.enum(["apagada", "inquieta", "tranquila", "feliz", "euforica"]);
export const Etapa = z.enum(["chispa", "llama", "fogata", "leyenda"]);
export const Situacion = z.enum(["saludo", "inactividad", "tension", "post_respiracion", "meta_cumplida", "subida_nivel"]);
/** Id aleatorio por reloj (UUID). No identifica a la persona. */
export const Dispositivo = z.string().regex(/^[0-9a-f-]{32,40}$/i, "dispositivo inválido");

export const MensajeEntrada = z.strictObject({
  dispositivo: Dispositivo,
  idioma: Idioma,
  situacion: Situacion,
  actividad: Nivel,
  emocion: Emocion,
  etapa: Etapa,
});

export const MetaEntrada = z.strictObject({
  dispositivo: Dispositivo,
  idioma: Idioma,
  /** Cómo cambió la meta que ya calculó el reloj con sus reglas. */
  tendencia: z.enum(["sube", "baja", "igual"]),
  /** Días de la semana con la meta cumplida, agregados. */
  constancia: Nivel,
  actividadMedia: Nivel,
});

export const ResumenEntrada = z.strictObject({
  dispositivo: Dispositivo,
  idioma: Idioma,
  actividad: Nivel,
  metaCumplida: z.boolean(),
  respiraciones: z.int().min(0).max(20),
  tension: z.enum(["ninguna", "alguna", "frecuente"]),
  emocionFinal: Emocion,
});

export const EstadoBrasa = z.strictObject({
  nivel: z.int().min(1).max(500),
  xp: z.int().min(0).max(10_000_000),
  etapa: Etapa,
  racha: z.int().min(0).max(3650),
});

/** Salidas de la IA (también se validan: si no cumplen, se usa el respaldo local). */
export const SalidaFrase = z.strictObject({ frase: z.string().min(2).max(140) });
export const SalidaResumen = z.strictObject({ frases: z.array(z.string().min(2).max(160)).length(2) });

export type TMensaje = z.infer<typeof MensajeEntrada>;
export type TMeta = z.infer<typeof MetaEntrada>;
export type TResumen = z.infer<typeof ResumenEntrada>;
export type TEstado = z.infer<typeof EstadoBrasa>;
