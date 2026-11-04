# Brasa — guía del proyecto

App de Wear OS (Kotlin + Compose for Wear OS) con una mascota de fuego que lee pasos y pulso y decide si moverte, calmarte o descansar. Proyecto académico de **Diseño de Interfaces** (entrega 8-nov-2026). **El diseño pesa más que la cantidad de funciones.**

## Reglas
- Standalone (`com.google.android.wearable.standalone=true`). minSdk 30, compile/targetSdk 36.
- **Prohibido `Modifier.blur`**: todos los brillos con degradados radiales.
- Todo color, tipo, espacio y duración sale de `app/src/main/kotlin/com/brasa/app/ui/theme/Tokens.kt`.
- Prohibido: Material por defecto sin personalizar, fondos grises, tarjetas planas, emojis, texto cortado en el borde.
- Textos en `res/values` (es por defecto) y `res/values-en`. Nada de strings en el código.
- Privacidad: pasos y pulso exactos **nunca** salen del reloj; al servidor solo niveles (bajo/medio/alto, pulso elevado en reposo).
- `GEMINI_API_KEY` solo en Vercel / `servidor/.env.local`. `SERVER_URL` en `local.properties` → BuildConfig.
- No es dispositivo médico: hablar de "señales de tensión", nunca de diagnósticos.
- Commit en español después de cada paso que compile.

## Entorno (Windows)
- Proyecto en `C:\dev\brasa` (fuera de OneDrive). Referencias de diseño en `docs/referencias/`.
- `JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"`, SDK en `%LOCALAPPDATA%\Android\Sdk`.
- `sdkmanager` está obsoleto: usar `cmdline-tools/latest/bin/android.exe` (`android sdk install`, `android docs search|fetch`).
- Emulador: AVD `Brasa_Wear_Redondo` (Wear OS 5, API 34, wearos_large_round). Necesita el driver AEHD instalado.
- Compilar: `./gradlew :app:assembleDebug`. Capturas: `./gradlew :app:recordRoborazziDebug` → `docs/capturas/roborazzi/`.

## Versiones verificadas (Google Maven / Maven Central, nov-2026)
AGP 9.0.1 · Gradle 9.1.0 · Kotlin 2.3.20 · Compose BOM 2026.03.01 · Wear Compose 1.7.1 · Health Services 1.1.0 · DataStore 1.3.0 · AppCompat 1.8.0 · kotlinx.serialization 1.12.0 · Roborazzi 1.77.0 · Robolectric 4.17.

## Permisos de salud (verificado en docs oficiales)
- Pasos: `ACTIVITY_RECOGNITION`.
- Pulso: `BODY_SENSORS` (maxSdk 35) + `android.permission.health.READ_HEART_RATE` (API 36+).
- Pasivo en segundo plano: `BODY_SENSORS_BACKGROUND` (33–35) + `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` (36+).
- Datos sintéticos en Wear OS 4+: ligados al ciclo de ejercicio; para pasivo usamos además una fuente simulada (modo demo).

## Arquitectura
```
app/src/main/kotlin/com/brasa/app/
  ui/theme/    Tokens.kt, Tipografia.kt, BrasaTheme.kt
  ui/brasa/    Emocion.kt, Etapa.kt, Pose.kt, DibujoBrasa.kt, Brasa.kt (composable animado)
  ui/pantallas/ galería (prueba), arranque, inicio, respirar, gráficas, evolución, ajustes
  domain/      reglas puras (emoción, tensión, XP, meta) con tests
  data/        Health Services, DataStore, cliente HTTP, frases locales
servidor/      TypeScript + Vercel + Zod + Gemini Flash-Lite
```

## Fases (estado)
- [x] 0 Entorno, git, proyecto (emulador creado; falta driver AEHD como admin)
- [x] 1 Dirección de arte, tokens, Brasa + 5 emociones + galería, revisión visual
- [x] 2 Arranque, inicio, respirar, ajustes y navegación (es/en, oscuro/claro)
- [ ] 3 Health Services y reglas
- [ ] 4 Gráficas y evolución
- [ ] 5 Servidor, Gemini, Vercel, modo demo
- [ ] 6 P1, pulido, accesibilidad
- [ ] 7 README, justificación, QA
