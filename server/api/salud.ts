import { aiAvailable, MODEL } from "../lib/ai.js";
import { storeType } from "../lib/store.js";
import { json } from "../lib/http.js";

/** GET /api/salud — comprueba que el servidor está vivo (no expone la clave). */
export function GET() {
  return json({ ok: true, ia: aiAvailable(), modelo: MODEL, almacen: storeType });
}
