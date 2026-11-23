# QA — Brasa 1.0.0

Leyenda: ✅ verificado (se indica cómo) · ⬜ solo lo puede probar el usuario (pasos exactos).
Fecha de la verificación: 23-nov-2026, rama `mejoras-v1`.

## Pruebas automáticas
| Caso | Estado | Cómo |
|---|---|---|
| Compila la app | ✅ | `./gradlew :app:assembleDebug` |
| 170 pruebas de la app en verde | ✅ | `./gradlew :app:testDebugUnitTest` (reglas 16, historial/respaldo 4, demo 2, pulso 1, interfaz 8, accesibilidad 5, sin servidor 6, capturas 128) |
| Capturas idénticas a las aprobadas | ✅ | `./gradlew :app:verifyRoborazziDebug` |
| 17 pruebas del servidor | ✅ | `cd servidor && npm test` |
| Tipos del servidor | ✅ | `npm run tipos` |

## Reglas
| Caso | Estado | Cómo |
|---|---|---|
| Emoción según % de meta, límites 20 / 60 / 100 % | ✅ | `ReglasTest.emocionSegunProgreso`, `emocionEnLosLimites` |
| Tensión solo con pulso alto **en reposo** y ≥ 2 lecturas en 3 min | ✅ | `ReglasTest` (4 casos) + emulador con el simulador (captura `emulador/v1/23`) |
| Moverse con el pulso alto no es tensión | ✅ | `pulsoAltoMoviendoseNoEsTension` + emulador: con pasos del Health Services, pulso 110–114 → tranquila (captura `08`) |
| XP, nivel, etapa, meta adaptativa ±15 %, racha | ✅ | `ReglasTest` |
| Meta y subida de nivel se celebran una sola vez | ✅ | `metaSeCelebraUnaSolaVezAlCruzarEl100`, `subidaDeNivelYEtapaNueva` |
| Demo nunca pinta horas futuras | ✅ | `DemoTest` (las 24 horas) |

## Interfaz y navegación
| Caso | Estado | Cómo |
|---|---|---|
| Deslizar recorre Inicio → Pasos → Pulso → Semana → Evolución → Ajustes y vuelve | ✅ | `InterfazTest.deslizarRecorreLasSeisPaginasEnOrden` + emulador |
| Tocar a Brasa la hace reír (y en modo manual suma 250 pasos) | ✅ | `InterfazTest.inicioTocarBrasaYBotonRespirar` |
| Respirar dura 1 min, muestra −14 lpm y "Listo" vuelve | ✅ | `InterfazTest.respirarDuraUnMinuto…` + emulador (capturas `16`–`19`) |
| Ajustes: tema, demo, emoción y etapa hacen lo que dicen | ✅ | `InterfazTest.ajustesCambian…` + emulador (tema claro, captura `20`) |
| Permisos: Permitir / Ahora no | ✅ | `InterfazTest.permisosPermitirYAhoraNo` + emulador |
| Subida de nivel: animación ~2,6 s, se cierra sola | ✅ | Emulador con `--ei pasos 6000` (captura `04`) + capturas v1 |
| Vibración al cumplir la meta y al subir de nivel | ✅ | Emulador: `adb shell dumpsys vibrator_manager` muestra el patrón con `usage: TOUCH` |
| Nada cortado en el borde (6 variantes) | ✅ | Revisión de las 128 capturas, ver `revision-visual.md` |
| Corona para pasar de página | ⬜ | En el emulador: barra lateral → *Extended controls* → *Rotary input*, o en un reloj real girar la corona en Inicio: debe pasar a Pasos |

## Servidor y red
| Caso | Estado | Cómo |
|---|---|---|
| Producción responde | ✅ | `curl https://brasa-servidor.vercel.app/api/salud` → `{"ok":true,"ia":true,…}` |
| Petición válida → frase de Gemini | ✅ | `curl` a `/api/mensaje` → `"origen":"ia"` (es y en) |
| Petición inválida → 400 | ✅ | `idioma: "fr"` → `entrada_invalida` |
| Datos exactos (`actividad: 8423`, `pulso: 112`) → 400 | ✅ | `curl` en producción + `npm test` |
| Cuerpo > 2.000 bytes → 413 | ✅ | `curl` en producción + `npm test` |
| La clave no aparece en ninguna respuesta | ✅ | `/api/salud` solo dice `ia: true` |
| Las frases del reloj llegan desde Vercel | ✅ | Emulador: `adb logcat -s BrasaServidor` → `/api/mensaje ok origen=ia` (captura `01`) |
| Sin internet → frases locales, sin errores ni esperas | ✅ | Emulador en modo avión: `UnknownHostException` capturada, frase local al instante (captura `02`) + `ServidorSinRedTest` |
| `SERVER_URL` vacío, servidor caído, 500/429, JSON roto → frases locales | ✅ | `ServidorSinRedTest` (6 casos) |

## Continuidad y varios relojes
| Caso | Estado | Cómo |
|---|---|---|
| Copia y restauración (mismo nivel y meta, sin datos de salud) | ✅ | Emulador: `bmgr transport …LocalTransport` → `bmgr backupnow com.brasa.app` → `pm clear` → `bmgr restore 1 com.brasa.app`: solo vuelve `brasa_respaldo.preferences_pb`, nivel 3 · 117 XP, se vuelven a pedir permisos (capturas `06`, `07`) |
| Dos relojes a la vez, independientes | ✅ | Dos emuladores (`-read-only`): ids distintos, estados distintos y frases de Vercel en ambos (captura `08`) |
| Restauración real con la cuenta de Google al cambiar de reloj | ⬜ | 1) En el reloj viejo: *Ajustes del sistema → Sistema → Copia de seguridad → Hacer copia ahora*. 2) Configurar el reloj nuevo con la misma cuenta y elegir restaurar. 3) Instalar Brasa: en Evolución debe aparecer el mismo nivel. |

## Accesibilidad
| Caso | Estado | Cómo |
|---|---|---|
| Áreas táctiles ≥ 48 dp en 11 pantallas (reloj de 192 dp) | ✅ | `AccesibilidadTest.todoLoTactilMideAlMenos48dp` |
| Todo lo táctil y toda imagen con nombre para TalkBack | ✅ | `AccesibilidadTest` |
| Contraste AA de todos los textos (oscuro y claro) | ✅ | `AccesibilidadTest.contrasteAADeLosTextos` |
| Reducir animaciones: Brasa quieta, nada roto | ✅ | `InterfazTest` corre con escala de animación 0 |
| TalkBack en un reloj | ⬜ | Activar *Ajustes → Accesibilidad → TalkBack*. En Inicio deslizar con un dedo: debe leer la hora, "Brasa, feliz, etapa Llama", "6.205 pasos de 8.500", la frase y "Meta de hoy al 73 por ciento". |
| Fuente grande en un reloj | ⬜ | *Ajustes → Pantalla → Tamaño de fuente* al máximo: ningún texto debe cortarse. |

## Pendiente de probar por el usuario
| Caso | Estado | Pasos |
|---|---|---|
| Sensores reales en un reloj físico | ⬜ | Instalar el APK, dar permisos y caminar 5 min: los pasos deben subir en Inicio y en Pasos. |
| Probar el APK entregado | ⬜ | `adb install -r entrega/Brasa-1.0.0.apk`; en *Ajustes del sistema → Apps → Brasa* debe verse la versión 1.0.0. |
