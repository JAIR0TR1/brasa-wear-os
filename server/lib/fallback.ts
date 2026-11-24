/** Respaldo del servidor cuando la IA no responde (el reloj además tiene su propio banco de frases). */
const R = {
  es: {
    saludo: ["¡A encender el día!", "Hola, aquí sigo ardiendo para ti."],
    inactividad: ["Me estoy apagando. ¿Caminamos un poco?", "Un paseo corto y vuelvo a brillar."],
    tension: ["Noto señales de tensión. ¿Respiramos un minuto?", "Vamos despacio, juntas. Un minuto de aire."],
    post_respiracion: ["Un minuto para ti. Mi llama ya está más tranquila.", "Bien hecho, respiramos al mismo ritmo."],
    meta_cumplida: ["¡Meta cumplida! ¡Ardo de alegría!", "¡Lo logramos! Qué hoguera tan bonita."],
    subida_nivel: ["¡Subí de nivel! Mi fuego crece contigo.", "¡Más grande y más brillante gracias a ti!"],
    meta_sube: "Subí tu meta un poco porque vienes de una gran semana.",
    meta_baja: "Bajé tu meta para que la alcances sin agobio.",
    meta_igual: "Tu meta sigue igual: vas a buen ritmo.",
    resumen: ["Hoy mantuvimos el fuego encendido.", "Mañana seguimos sumando leña juntas."],
  },
  en: {
    saludo: ["Let's light up the day!", "Hi, still burning bright for you."],
    inactividad: ["I'm fading. Shall we walk a bit?", "A short walk and I'll glow again."],
    tension: ["I sense signs of tension. Breathe with me?", "Let's slow down together. One minute of air."],
    post_respiracion: ["A minute for you. My flame feels calmer.", "Well done, we breathed in rhythm."],
    meta_cumplida: ["Goal reached! I'm blazing with joy!", "We did it! What a lovely bonfire."],
    subida_nivel: ["I leveled up! My fire grows with you.", "Bigger and brighter thanks to you!"],
    meta_sube: "I nudged your goal up after a great week.",
    meta_baja: "I lowered your goal so it feels reachable.",
    meta_igual: "Your goal stays the same: nice rhythm.",
    resumen: ["Today we kept the fire alive.", "Tomorrow we add more firewood together."],
  },
} as const;

export type FallbackSituation = "saludo" | "inactividad" | "tension" | "post_respiracion" | "meta_cumplida" | "subida_nivel";

export const localPhrase = (language: "es" | "en", s: FallbackSituation) => {
  const list = R[language][s];
  return list[Math.floor(Math.random() * list.length)]!;
};
export const localGoalPhrase = (language: "es" | "en", trend: "sube" | "baja" | "igual") => R[language][`meta_${trend}`];
export const localSummary = (language: "es" | "en") => [...R[language].resumen];
