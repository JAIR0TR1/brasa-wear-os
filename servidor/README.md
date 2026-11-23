# Brasa · servidor

Funciones de Vercel en TypeScript que dan voz a Brasa con **Gemini Flash-Lite**. El reloj funciona sin este servidor (tiene sus propias frases); el servidor solo hace que Brasa hable con más variedad.

## Privacidad: qué recibe y qué no
- **Recibe solo niveles y valores cerrados**: idioma (`es`/`en`), situación, actividad `bajo|medio|alto`, emoción, etapa y un id aleatorio por reloj (UUID, sin cuenta ni nombre).
- **Nunca recibe pasos ni pulso exactos.** Los esquemas Zod son estrictos: cualquier campo de más (`pasos`, `pulso`…) o texto libre se rechaza con `400`.
- El id del reloj **no se envía a Gemini**.
- No se registran cuerpos de petición: los registros solo guardan método, ruta, estado y el tipo de fallo de la IA.
- Cuerpos de más de 2.000 bytes: `413`. Límite de 20 peticiones/min por reloj y 60/min por IP: `429`.

## Rutas
| Método | Ruta | Para qué |
|---|---|---|
| GET | `/api/salud` | Comprueba que está vivo (`ia: true/false`, sin exponer la clave) |
| POST | `/api/mensaje` | Una frase de Brasa para una situación (≤ 56 caracteres para que quepa en 2 líneas del reloj) |
| POST | `/api/meta` | Explica en una frase el cambio de meta (la meta la calcula el reloj) |
| POST | `/api/resumen` | Resumen del día en 2 frases (preparado; el reloj aún no lo usa) |
| GET/PUT | `/api/brasa/{id}` | Nivel, XP, etapa y racha (en memoria; ver limitaciones) |

Ejemplo:
```bash
curl -s https://<proyecto>.vercel.app/api/mensaje -H "content-type: application/json" \
  -d '{"dispositivo":"3f6c2a1e-9b4d-4c7a-8e21-5d0b9a7f1c33","idioma":"es","situacion":"tension","actividad":"medio","emocion":"inquieta","etapa":"llama"}'
# {"frase":"Respira hondo conmigo y deja ir la tensión.","origen":"ia"}
```
Si Gemini falla, tarda más de 7 s, agota la cuota o devuelve algo que no cabe, la respuesta sigue siendo `200` con `"origen":"local"` (frase de respaldo).

## Ejecutar en local
```bash
cd servidor
npm install
cp .env.example .env.local   # y pega tu GEMINI_API_KEY (este archivo no se sube a git)
npm run dev                  # http://localhost:3000 · desde el emulador: http://10.0.2.2:3000
npm test                     # pruebas sin red ni clave (Gemini simulado)
npm run probar               # prueba real contra Gemini con la clave de .env.local
npm run tipos                # comprobación de tipos
```

## Desplegar en Vercel
```bash
cd servidor
npx vercel link --yes --project brasa-servidor
npx vercel env add GEMINI_API_KEY production   # pega la clave cuando la pida
npx vercel deploy --prod
```
Después, en la raíz del proyecto Android, `local.properties`: `SERVER_URL=https://<proyecto>.vercel.app`.

## Tecnologías
TypeScript · Vercel Functions (Node.js, `maxDuration` 10 s) · `@google/genai` 2.32.0 (`gemini-3.5-flash-lite`, salida JSON con esquema) · Zod 4.6.7 · pruebas con `node:test` + `tsx`.

## Limitaciones conocidas
- El límite de peticiones y `/api/brasa/{id}` viven en memoria: cada instancia de Vercel tiene la suya y se pierde al reiniciarse. Mejora futura: Upstash Redis (plan gratuito del Marketplace de Vercel).
- Plan gratuito de Gemini: límite por minuto. El reloj pide frases solo al cambiar de situación (caché + ≥ 20 s entre peticiones).
