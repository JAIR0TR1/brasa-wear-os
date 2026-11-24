/**
 * Servidor local para desarrollo (sin Vercel): enruta como Vercel a los handlers de /api.
 *   npm run dev   →  http://localhost:3000   (desde el emulador: http://10.0.2.2:3000)
 */
import { createServer } from "node:http";
import * as health from "./api/salud.js";
import * as message from "./api/mensaje.js";
import * as goal from "./api/meta.js";
import * as summary from "./api/resumen.js";
import * as brasa from "./api/brasa/[dispositivo].js";

type HandlerModule = Record<string, unknown>;
const routes: [RegExp, HandlerModule][] = [
  [/^\/api\/salud$/, health],
  [/^\/api\/mensaje$/, message],
  [/^\/api\/meta$/, goal],
  [/^\/api\/resumen$/, summary],
  [/^\/api\/brasa\/[^/]+$/, brasa],
];

const PORT = Number(process.env.PORT ?? 3000);

createServer(async (req, res) => {
  const url = new URL(req.url ?? "/", `http://${req.headers.host}`);
  const routeModule = routes.find(([pattern]) => pattern.test(url.pathname))?.[1];
  const handler = routeModule?.[req.method ?? "GET"] as ((r: Request) => Promise<Response> | Response) | undefined;
  if (!handler) { res.writeHead(404, { "content-type": "application/json" }).end('{"error":{"codigo":"no_encontrado"}}'); return; }
  const chunks: Buffer[] = [];
  for await (const chunk of req) chunks.push(chunk as Buffer);
  const body = chunks.length ? Buffer.concat(chunks) : undefined;
  const request = new Request(url, { method: req.method, headers: req.headers as Record<string, string>, body });
  const response = await handler(request);
  res.writeHead(response.status, Object.fromEntries(response.headers)).end(await response.text());
  console.log(req.method, url.pathname, response.status); // sin cuerpos: no se registran datos
}).listen(PORT, () => console.log(`Brasa servidor local en http://localhost:${PORT}`));
