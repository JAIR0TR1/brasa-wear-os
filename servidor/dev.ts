/**
 * Servidor local para desarrollo (sin Vercel): enruta como Vercel a los handlers de /api.
 *   npm run dev   →  http://localhost:3000   (desde el emulador: http://10.0.2.2:3000)
 */
import { createServer } from "node:http";
import * as salud from "./api/salud.js";
import * as mensaje from "./api/mensaje.js";
import * as meta from "./api/meta.js";
import * as resumen from "./api/resumen.js";
import * as brasa from "./api/brasa/[dispositivo].js";

type Modulo = Record<string, unknown>;
const rutas: [RegExp, Modulo][] = [
  [/^\/api\/salud$/, salud],
  [/^\/api\/mensaje$/, mensaje],
  [/^\/api\/meta$/, meta],
  [/^\/api\/resumen$/, resumen],
  [/^\/api\/brasa\/[^/]+$/, brasa],
];

const PUERTO = Number(process.env.PORT ?? 3000);

createServer(async (req, res) => {
  const url = new URL(req.url ?? "/", `http://${req.headers.host}`);
  const modulo = rutas.find(([r]) => r.test(url.pathname))?.[1];
  const handler = modulo?.[req.method ?? "GET"] as ((r: Request) => Promise<Response> | Response) | undefined;
  if (!handler) { res.writeHead(404, { "content-type": "application/json" }).end('{"error":{"codigo":"no_encontrado"}}'); return; }
  const trozos: Buffer[] = [];
  for await (const t of req) trozos.push(t as Buffer);
  const cuerpo = trozos.length ? Buffer.concat(trozos) : undefined;
  const peticion = new Request(url, { method: req.method, headers: req.headers as Record<string, string>, body: cuerpo });
  const r = await handler(peticion);
  res.writeHead(r.status, Object.fromEntries(r.headers)).end(await r.text());
  console.log(req.method, url.pathname, r.status); // sin cuerpos: no se registran datos
}).listen(PUERTO, () => console.log(`Brasa servidor local en http://localhost:${PUERTO}`));
