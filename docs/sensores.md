# Sensores y datos sintéticos

## Qué se lee (Health Services, modo pasivo)
| Dato | Tipo | Permiso |
|---|---|---|
| Pasos de hoy | `DataType.STEPS_DAILY` | `ACTIVITY_RECOGNITION` |
| Pulso | `DataType.HEART_RATE_BPM` | `BODY_SENSORS` (≤ API 35) · `android.permission.health.READ_HEART_RATE` (≥ API 36) |

- Antes de registrar se consultan las **capacidades** (`getCapabilitiesAsync`); solo se piden los tipos que el reloj soporta.
- Se usa `setPassiveListenerCallback`: llegan datos **mientras la app está abierta**. Limitación conocida: para datos con la app cerrada haría falta `PassiveListenerService` + `READ_HEALTH_DATA_IN_BACKGROUND` + reinscripción tras reinicio (WorkManager). Fuera de alcance por tiempo.
- Sin permisos o sin sensores → **modo manual**: tocar a Brasa le da 250 pasos de "leña"; las reglas funcionan igual.
- Los datos exactos se guardan solo en el reloj (DataStore). Nunca se envían.

## Fórmulas (`domain/Reglas.kt`, probadas en `ReglasTest`)
- **Emoción**: pulso elevado en reposo → inquieta; meta ≥ 100 % → eufórica; meta < 20 % o ≥ 3 h quieta → apagada; < 60 % → tranquila; si no → feliz.
- **Base en reposo**: percentil 20 del pulso del día (65 lpm si hay < 10 muestras).
- **Tensión**: media de los últimos 3 min ≥ base + 20 lpm (≥ 2 lecturas) **y** sin pasos en 5 min. Moverse con pulso alto no es tensión.
- **XP**: 1 por cada 100 pasos + 10 por respiración. **Nivel** = 1 + ⌊√(XP/25)⌋ (nivel 5 ≈ 1 semana activa, 10 ≈ 1 mes, 20 ≈ 3,5 meses).
- **Meta adaptativa**: media de los últimos 7 días × 1,05, redondeada a 100, cambio máximo ±15 %/día, rango 3.000–15.000.
- **Racha**: días consecutivos con ≥ 60 % de la meta.

## Probar en el emulador
En Wear OS 4+ los datos sintéticos del emulador están ligados a sesiones de **ejercicio** (docs: *Simulate sensor data with Health Services*) y no alimentan el modo pasivo. Por eso la versión de depuración incluye un **simulador por adb**:

```bash
# Caminar: subir los pasos de hoy
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 2500
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 6500   # → feliz
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 9000   # → eufórica
# Pulso alto en reposo (esperar 5 min sin pasos, o desde el arranque sin pasos)
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pulso 112     # → inquieta + botón Respirar
```
El receptor solo existe en `src/debug` (no se incluye en la versión release).
