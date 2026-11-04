import { iaDisponible, MODELO } from "../lib/ia.js";
import { tipoAlmacen } from "../lib/almacen.js";
import { json } from "../lib/http.js";

/** GET /api/salud — comprueba que el servidor está vivo (no expone la clave). */
export function GET() {
  return json({ ok: true, ia: iaDisponible(), modelo: MODELO, almacen: tipoAlmacen });
}
