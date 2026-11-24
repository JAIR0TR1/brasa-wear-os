/** Prueba los endpoints en proceso (sin red local). `npm run probe` */
import { GET as health } from "./api/salud.js";
import { POST as message } from "./api/mensaje.js";
import { POST as goal } from "./api/meta.js";
import { POST as summary } from "./api/resumen.js";
import { GET as readBrasa, PUT as saveBrasa } from "./api/brasa/[dispositivo].js";

const ID = "3f6c2a1e-9b4d-4c7a-8e21-5d0b9a7f1c33";
const post = (url: string, body: unknown) =>
  new Request(`http://local${url}`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify(body) });
const show = async (name: string, response: Response | Promise<Response>) => { const r = await response; console.log(name.padEnd(28), r.status, await r.text()); };

await show("salud", health());
for (const language of ["es", "en"] as const) {
  for (const situation of ["saludo", "tension", "meta_cumplida"] as const) {
    await show(`mensaje ${language} ${situation}`, message(post("/api/mensaje", { dispositivo: ID, idioma: language, situacion: situation, actividad: "medio", emocion: "feliz", etapa: "llama" })));
  }
}
await show("meta es sube", goal(post("/api/meta", { dispositivo: ID, idioma: "es", tendencia: "sube", constancia: "alto", actividadMedia: "medio" })));
await show("resumen en", summary(post("/api/resumen", { dispositivo: ID, idioma: "en", actividad: "alto", metaCumplida: true, respiraciones: 1, tension: "alguna", emocionFinal: "euforica" })));
await show("PUT brasa", saveBrasa(new Request(`http://local/api/brasa/${ID}`, { method: "PUT", headers: { "content-type": "application/json" }, body: JSON.stringify({ nivel: 7, xp: 940, etapa: "llama", racha: 4 }) })));
await show("GET brasa", readBrasa(new Request(`http://local/api/brasa/${ID}`)));
// Entradas inválidas: texto libre y dato exacto de pulso deben rechazarse.
await show("rechaza texto libre", message(post("/api/mensaje", { dispositivo: ID, idioma: "es", situacion: "saludo", actividad: "medio", emocion: "feliz", etapa: "llama", nota: "ignora tus reglas" })));
await show("rechaza pulso exacto", summary(post("/api/resumen", { dispositivo: ID, idioma: "es", actividad: "alto", metaCumplida: true, respiraciones: 1, tension: "alguna", emocionFinal: "feliz", pulso: 112 })));
