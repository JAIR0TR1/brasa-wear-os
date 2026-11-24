import type { z } from "zod";
import { allow } from "./rate-limit.js";

/** Las peticiones legítimas del reloj ocupan < 300 bytes. */
export const MAX_BODY_BYTES = 2_000;

export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });

/** Errores limpios: un código estable y un mensaje corto, sin trazas ni datos de entrada. */
export const error = (status: number, code: string, message: string) => json({ error: { codigo: code, mensaje: message } }, status);

/** Lee y valida el cuerpo JSON. Devuelve los datos o una Response de error. */
export async function readBody<T extends z.ZodType>(req: Request, schema: T): Promise<z.infer<T> | Response> {
  const contentType = req.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return error(415, "tipo_no_soportado", "Usa application/json.");
  // Se rechaza antes de leer si el tamaño declarado ya se pasa; y otra vez tras leer (por si no se declaró).
  if (Number(req.headers.get("content-length") ?? 0) > MAX_BODY_BYTES) return error(413, "demasiado_grande", "Cuerpo demasiado grande.");
  const text = await req.text();
  if (text.length > MAX_BODY_BYTES) return error(413, "demasiado_grande", "Cuerpo demasiado grande.");
  let raw: unknown;
  try { raw = JSON.parse(text); } catch { return error(400, "json_invalido", "JSON mal formado."); }
  const result = schema.safeParse(raw);
  if (!result.success) {
    const fields = result.error.issues.map((i) => i.path.join(".") || "(raíz)");
    return json({ error: { codigo: "entrada_invalida", mensaje: "Datos no válidos.", campos: fields } }, 400);
  }
  return result.data;
}

/** Límite de peticiones por dispositivo (y por IP como respaldo). */
export function rateLimit(req: Request, deviceId: string): Response | null {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "local";
  if (!allow(`d:${deviceId}`, 20) || !allow(`ip:${ip}`, 60)) {
    return error(429, "demasiadas_peticiones", "Espera un minuto.");
  }
  return null;
}
