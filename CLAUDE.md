# Brasa — guía del proyecto

**Versión 1.0.0** (versionCode 2). App de Wear OS (Kotlin + Compose for Wear OS) con una mascota de fuego que lee pasos y pulso y decide si moverte, calmarte o descansar. Proyecto académico de **Diseño de Interfaces** (entrega 8-nov-2026). **El diseño pesa más que la cantidad de funciones.**

## Reglas
- Standalone (`com.google.android.wearable.standalone=true`). minSdk 30, targetSdk 36, compileSdk 37.
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
- Compilar: `./gradlew :app:assembleDebug` (**sin `-q`**: con `-q` + configuration cache se instaló un APK viejo).
- Emulador: `emulator -avd Brasa_Wear_Redondo`; app en español: `adb shell cmd locale set-app-locales com.brasa.app --locales es`. En Git Bash usar `MSYS_NO_PATHCONV=1` con rutas `/sdcard`. Capturas: `./gradlew :app:recordRoborazziDebug` → `docs/capturas/roborazzi/`.

## Versiones en uso (las del repositorio, nov-2026)
AGP 9.1.1 · Gradle 9.3.1 · Kotlin 2.3.20 · Compose BOM 2026.03.01 · Wear Compose 1.7.1 · Health Services 1.1.0 · DataStore 1.3.0 · AppCompat 1.8.0 · kotlinx.serialization 1.12.0 · Roborazzi 1.77.0 · Robolectric 4.17.

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
  domain/      reglas puras (emoción, tensión, XP, meta, celebraciones) con tests
  data/        Health Services, DataStore, cliente HTTP, frases locales
servidor/      TypeScript + Vercel + Zod + Gemini Flash-Lite (pruebas en servidor/pruebas, `npm test`)
```
- DataStore "brasa" (solo en el reloj, fuera de la copia): registro del día con pasos/pulso, historial e id anónimo.
- DataStore "brasa_respaldo" (Auto Backup, `res/xml/reglas_extraccion.xml` y `respaldo_completo.xml`): ajustes, XP total, respiraciones y meta.

## Fases (estado)
- [x] 0 Entorno, git, proyecto
- [x] 1 Dirección de arte, tokens, Brasa + 5 emociones + galería, revisión visual
- [x] 2 Arranque, inicio, respirar, ajustes y navegación (es/en, oscuro/claro)
- [x] 3 Health Services (callback pasivo), reglas y tests, permisos, modo manual, simulador adb
- [x] 4 Gráficas (pasos radial, pulso, semana) y evolución; probado en emulador
- [x] 5 Servidor desplegado en Vercel: https://brasa-servidor.vercel.app (proyecto `brasa-servidor`, `GEMINI_API_KEY` como variable sensible de producción). Reloj conectado (UUID anónimo, solo niveles).
- [x] 6 Háptica de celebración, animación de subida de nivel, copia de seguridad (Auto Backup), accesibilidad (TalkBack, 48 dp, AA) y pulido
- [x] 7 README, docs/justificacion-diseno.md, docs/QA.md, docs/portafolio.md, docs/demo-profesor.md, docs/screenshots/

Mejoras futuras (no hechas a propósito):
- **Upstash Redis** para `/api/brasa/{dispositivo}` y el límite de peticiones (hoy en memoria por instancia de Vercel).
- Resumen del día con IA en el reloj (el servidor ya tiene `/api/resumen`).
- Datos con la app cerrada (`PassiveListenerService` + `READ_HEALTH_DATA_IN_BACKGROUND`).
- Tile de Wear OS.

## Gemini (verificado nov-2026)
- SDK `@google/genai` 2.32.0, `ai.models.generateContent` con `responseMimeType` + `responseJsonSchema` (de `z.toJSONSchema`).
- La API rechaza `httpOptions.timeout` < 10 s → se usa `abortSignal` a 7 s. Latencia medida ≈ 1 s.
- Plan gratuito: límite por minuto (429 al hacer ~40 llamadas seguidas). El reloj pide frases solo al cambiar de situación (caché + ≥ 20 s).
- Frases ≤ 9 palabras pedidas, ≤ 12 palabras / 56 caracteres aceptadas (`MAX_CARACTERES`, 2 líneas en el reloj de 192 dp; además la app reduce la letra hasta 11 sp antes de cortar). El servidor pone mayúscula inicial.
- La clave vive solo en `servidor/.env.local` (ignorado) y en Vercel (variable sensible de producción).
- `vercel link` conectó el repositorio de GitHub al proyecto: los despliegues se hacen con `npx vercel deploy --prod` desde `servidor/`.

## Dónde quedamos (23-nov-2026)
Versión 1.0.0 terminada en la rama `mejoras-v1` (sin unir a `main`). 170 pruebas de la app + 17 del servidor en verde; servidor en producción; APK en `entrega/Brasa-1.0.0.apk` (debug: incluye el simulador adb para la demo).
Pendiente del usuario: probar en un reloj real, TalkBack y fuente grande (ver docs/QA.md ⬜), decidir si se une `mejoras-v1` a `main`, y rotar la clave de Gemini después de la entrega.

## Cómo retomar
- Abrir la carpeta `C:\dev\brasa` en VS Code y abrir Claude Code ahí.
- Emulador: `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Brasa_Wear_Redondo`
- Servidor local: `cd servidor && npm run dev` (puerto 3000; si "EADDRINUSE", cerrar el node viejo).

## Reglas de trabajo y lecciones aprendidas
- Respetar todas las reglas de este archivo: tokens en Tokens.kt, sin Modifier.blur, sin strings en el código (es y en), privacidad, no es dispositivo médico.
- No cambiar la dirección de arte, la mascota, la arquitectura ni las versiones de librerías. No agregar funciones que no se pidan.
- NUNCA abrir, leer, buscar ni mostrar claves: ni servidor/.env.local, ni notas o archivos de texto del usuario, ni variables con claves. Para usarlas, pasarlas por comandos que no impriman el valor.
- Trabajar en la rama "mejoras-v1". Commits en español tras cada paso que compile y pase pruebas.
- Después de cada paso: ./gradlew :app:assembleDebug (sin -q), ./gradlew :app:testDebugUnitTest y ./gradlew :app:verifyRoborazziDebug (record solo si el cambio visual es intencional, revisando las capturas). Si algo se rompe, revertir.
- Si se necesita algo del usuario, detenerse y decirle exactamente qué hacer, en pasos simples.
- Nada de relleno: sin textos de ejemplo presentados como reales, botones que no hacen nada ni pantallas vacías. Si algo no está listo, se oculta.
- No inventar datos, métricas ni funciones. En modo demo los datos deben ser creíbles y estar marcados como demo.
- Nada cortado en el borde: revisar cada texto en redondo grande, redondo pequeño, cuadrado, fuente grande (×1,3), inglés y tema claro.
- Cada toque y gesto hace lo que dice. Áreas táctiles ≥ 48 dp.
- Animaciones fluidas que respetan "reducir movimiento" sin dejar la pantalla rota o vacía.
- Accesibilidad: contraste AA, descripciones para TalkBack, orden de lectura lógico.
- Coherencia: nombre, versión, descripción y tecnologías iguales en la app, README, CLAUDE.md y portafolio.
- "Listo" solo cuando está VERIFICADO (Roborazzi + emulador), explicando cómo se comprobó. Si un cambio no se ve en el emulador, confirmar que se instaló el APK nuevo.
- La app debe funcionar SIEMPRE, aunque el servidor o internet fallen.
- Usar las skills disponibles cuando ayuden (diseño de interfaces, pruebas, revisión de código), respetando este archivo, y decir cuáles se usaron.
