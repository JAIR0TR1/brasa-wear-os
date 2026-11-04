import type { z } from "zod";
import { permitir } from "./limite.js";

export const json = (cuerpo: unknown, estado = 200) =>
  new Response(JSON.stringify(cuerpo), {
    status: estado,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });

/** Errores limpios: un código estable y un mensaje corto, sin trazas ni datos de entrada. */
export const error = (estado: number, codigo: string, mensaje: string) => json({ error: { codigo, mensaje } }, estado);

/** Lee y valida el cuerpo JSON. Devuelve los datos o una Response de error. */
export async function leer<T extends z.ZodType>(req: Request, esquema: T): Promise<z.infer<T> | Response> {
  const tipo = req.headers.get("content-type") ?? "";
  if (!tipo.includes("application/json")) return error(415, "tipo_no_soportado", "Usa application/json.");
  const texto = await req.text();
  if (texto.length > 2_000) return error(413, "demasiado_grande", "Cuerpo demasiado grande.");
  let bruto: unknown;
  try { bruto = JSON.parse(texto); } catch { return error(400, "json_invalido", "JSON mal formado."); }
  const r = esquema.safeParse(bruto);
  if (!r.success) {
    const campos = r.error.issues.map((i) => i.path.join(".") || "(raíz)");
    return json({ error: { codigo: "entrada_invalida", mensaje: "Datos no válidos.", campos } }, 400);
  }
  return r.data;
}

/** Límite de peticiones por dispositivo (y por IP como respaldo). */
export function limitar(req: Request, dispositivo: string): Response | null {
  const ip = req.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ?? "local";
  if (!permitir(`d:${dispositivo}`, 20) || !permitir(`ip:${ip}`, 60)) {
    return error(429, "demasiadas_peticiones", "Espera un minuto.");
  }
  return null;
}
