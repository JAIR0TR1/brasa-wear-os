# Revisión visual

Capturas: `docs/capturas/roborazzi/` (Roborazzi + Robolectric, `./gradlew :app:recordRoborazziDebug`).
Variantes: redondo grande (227 dp), redondo pequeño (192 dp), cuadrado (198 dp), tema claro, inglés y fuente grande (×1,3).

Criterios: jerarquía · nada cortado en el borde · contraste AA · Brasa protagonista y expresiva · emociones distinguibles · tokens · espaciado · textos cortos · claro con identidad · inglés · áreas táctiles · ¿parece plantilla?

---

## Fase 1 — Brasa, emociones y etapas

### Ronda 1
| Problema | Criterio | Corrección |
|---|---|---|
| Todas las capturas en inglés (Robolectric usa en-US) | idiomas | Calificador `es-` en las variantes base |
| Silueta lisa y simétrica: se lee como **gota de agua**, no fuego; el brillo blanco central da aspecto de cristal | ¿plantilla? / personalidad | Dos lenguas secundarias unidas a la silueta (`Path.op` Union) → tres puntas; punta más inclinada; brillo interior y especular atenuados |
| Cara pequeña para un reloj | Brasa expresiva | Ojos +20 % y más separados, boca +24 %, mejillas desplazadas |
| Texto curvo pegado al borde; la corona de Leyenda lo pisa | nada cortado | `CurvedModifier.padding(8 dp)` y más aire superior |
| Llamas laterales escondidas tras los brazos, con forma de espina | evolución legible | Más grandes y bajas |

Lo que ya funcionaba: las 5 emociones se distinguen **por forma, no solo por color** (brazos colgando / manos a la boca / vaivén / saludo / brazos arriba), el halo radial sobre negro da "fuego en la oscuridad", el tema claro conserva identidad (halo cálido + borde marcado en lugar de brillo).

### Ronda 2
| Problema | Criterio | Corrección |
|---|---|---|
| La corona de Leyenda aún roza el título curvo | nada cortado | Con corona Brasa baja 9u y escala 0,96 |
| Llamas laterales de Fogata parecen **alas de murciélago** | ¿plantilla? / lectura | Nacen a ras de suelo junto a los pies y suben casi verticales (fogata real) |
| "meta < 20 % · sin moverte" casi toca el borde | textos cortos | "meta < 20 % · quieta" |

### Ronda 3
| Problema | Criterio | Corrección |
|---|---|---|
| En pantalla cuadrada el texto curvo no tiene sentido y la corona choca con el título | cuadrado | Componente `TituloBorde`: curvo si `isScreenRound`, recto si no |

**Resultado:** pasa. Pendiente para fase 2: indicador de página en cuadrado (se ve como arco; aceptable), áreas táctiles se validan en pantallas con botones.

### Ronda 4 (lámina completa)
| Problema | Criterio | Corrección |
|---|---|---|
| Con fuente ×1,3 "pulso alto en reposo" se sale del círculo (en 192 dp, al límite) | nada cortado / fuente grande | Subtítulo con margen lateral extra y hasta 2 líneas en lugar de recortarse |

**Resultado final fase 1:** pasa en las 6 variantes. Lámina: `docs/capturas/lamina-fase1.png`.

---

## Fase 2 — Arranque, Inicio, Respirar, Ajustes

### Ronda 1
| Problema | Criterio | Corrección |
|---|---|---|
| En Inicio Brasa queda pequeña y la cifra de pasos domina | Brasa protagonista / jerarquía | Hueco fijo para la hora, Brasa a todo el ancho, cifra 30 → 25 sp |
| Con fuente grande la hora curva pisa la punta de la llama | nada cortado | Espacio superior fijo (36 dp) independiente del tamaño de letra |
| Ajustes: filas tocan el borde del círculo y el título flota en el centro | márgenes seguros | `contentPadding` lateral 14 dp, lista empieza arriba (`autoCentering = null`) |
| Resultado "-14" con guion | tipografía | Signo menos real "−" |

Lo que ya funcionaba: el remolino de anillos opuestos del arranque, la respiración (Brasa dentro de ondas que crecen con ella y fondo que "respira"), el botón Respirar en inquieta (no depende solo del color: icono + texto), tema claro con identidad.

### Ronda 2
| Problema | Criterio | Corrección |
|---|---|---|
| Brasa aún algo pequeña en Inicio | protagonista | Escala ×1,14 creciendo hacia abajo (origen arriba) |
| Eufórica: salto + punto de luz del anillo al 100 % pisan la hora | nada cortado | Hora con margen 20 dp; Brasa crece hacia abajo |
| Una chispa de eufórica roza la hora | nada cortado | Órbita de chispas más lateral (factor vertical 0,9 → 0,62) |

### Ronda 3 (lámina completa)
| Problema | Criterio | Corrección |
|---|---|---|
| Frases de una línea llegan hasta el anillo | márgenes seguros | Margen lateral de la frase 8 → 16 dp |

**Resultado fase 2:** pasa. Lámina: `docs/capturas/lamina-fase2.png`.

---

## Fase 3 — Prueba en emulador (Wear OS 5, API 34) con Health Services real + simulador adb
| Hallazgo | Corrección |
|---|---|
| El saludo nunca se cambiaba aunque cambiara la emoción | Saludo de 6 s; frase nueva en cada cambio de emoción |
| Punto de luz del anillo brillando con 10 pasos (sin arco visible) | Por debajo del 1 % no se dibuja |
| Tensión no se activaba con el simulador: se guarda 1 pulso/min y la regla exige ≥ 2 lecturas en 3 min | Simulador espaciado a 70 s (con sensor real se cumple sola) |
| Resultado de respirar "+20 lpm" presentado como logro (el HS del emulador genera pulso sintético ~100–125) | Si el pulso no baja: "Un minuto para ti. Eso ya cuenta." y el dato en gris |
| Permisos: el botón Permitir quedaba bajo el pliegue | Texto más corto; acción principal visible sin desplazar |

## Fase 4 — Gráficas y evolución

### Ronda 1 (Roborazzi, datos demo)
| Problema | Criterio | Corrección |
|---|---|---|
| Demo pintaba pasos en horas futuras | coherencia | El demo solo rellena hasta la hora actual |
| El pico de pulso no tenía cifra | jerarquía | Cifra sobre el punto (TextMeasurer en Canvas) |
| XP sin separador de miles ("3065") | idiomas | `NumberFormat` según el idioma |

### Ronda 2 (lámina)
| Problema | Criterio | Corrección |
|---|---|---|
| Pulso vacío: pantalla casi negra y eje cortado | estados vacíos | `EstadoVacio`: Brasa tranquila + mensaje + pista |
| Pasos en 192 dp / fuente grande: centro choca con las barras | nada cortado | Centro compacto (26 sp) con 0 h/12 h dentro |
| Frase de la meta truncada en 192 dp | textos cortos | Frases más cortas |

### Ronda 3 (emulador con datos reales)
| Problema | Criterio | Corrección |
|---|---|---|
| Semana con un solo día: barra gigante y "0 de 0 días" | estados vacíos | Siempre 7 huecos; "Tu primer día con Brasa" |
| Chip "+20 lpm tras respirar" con pulso que subió | honestidad | Solo se muestra si bajó; hueco reservado para que el eje no se mueva |
| Evolución: última línea cortada en el borde | nada cortado | Margen lateral extra |

**Resultado fase 4:** pasa. Lámina: `docs/capturas/lamina-fase4.png`; emulador: `docs/capturas/emulador/`.

---

## Versión 1.0.0 — revisión final (23-nov-2026)

### Ronda 1 (Roborazzi, 6 variantes de todas las pantallas)
| Problema | Criterio | Corrección |
|---|---|---|
| Capturas de Semana dependían del día real (letras de los días): verify fallaba cada día | pruebas fiables | `GraficaSemana(hoy = …)` y demo con minuto fijo en las capturas |
| Pulso: "0 h" y "24 h" pegados al borde en pantallas pequeñas | nada cortado | Eje con margen lateral de 12 dp; las etiquetas se alinean con el área de datos |
| Pulso con fuente grande: el chip "−16 lpm tras respirar" rozaba el círculo | nada cortado / fuente grande | Chip con margen y texto que se ajusta al ancho (autoSize 9–12 sp) |
| Pasos: la barra más larga y su brillo llegaban al borde (y al indicador de páginas) | márgenes seguros | Token `bordeSeguro` (18 dp), barras 32 % del radio, brillo más corto |
| Pasos en 192 dp / cuadrado / fuente grande: "pico a las 18:00" tocaba las barras | nada cortado | El centro se mide con el radio interior real del anillo y se ajusta (autoSize) |
| Pulso en demo: curva fija de 6 a 22 h aunque fuera de mañana | coherencia demo | Demo cada 5 min de 00:00 hasta la hora actual (sueño, caminatas, pico de tensión 16:10, respiración 16:25 solo si ya pasó) |
| Pulso con pocos datos reales: línea de 2 puntos sin sentido | estados vacíos | Estado "Midiendo tu pulso" con la última lectura (< 6 muestras o < 20 min) |
| Semana el primer día: "Tu meta sigue igual" no explica nada y el encabezado se cortaba | estados vacíos / nada cortado | "Cada día suma una barra." y encabezado con autoSize |
| Semana en tema claro: línea de meta dorada casi invisible | contraste | Línea con el fuego del tema claro |
| Inicio: frases de una línea tocaban el anillo | nada cortado | Margen 24 dp; si no caben en 2 líneas se reducen (hasta 11 sp) en lugar de cortarse. El servidor acepta como mucho 56 caracteres |
| Permisos en 192 dp / cuadrado / fuente grande: "Permitir" cortado abajo | acción principal visible | Brasa más compacta y texto más corto |

**Resultado:** pasa en las 6 variantes. 121 pruebas (unitarias + capturas) en verde.

### Ronda 2 (emulador Wear OS 5, API 34: demo, simulador, Vercel y sin internet)
| Hallazgo | Corrección |
|---|---|
| La frase de "inactividad" nunca se pedía a la IA: caía dentro de la ventana de 20 s del saludo y no se reintentaba | Reintento al cumplirse la ventana si la situación sigue igual |
| Gemini a veces empezaba la frase en minúscula | El servidor pone mayúscula inicial (también tras ¡ y ¿) |
| Pulso en un día tranquilo: la línea de umbral (reposo + 20) se dibujaba encima del texto | El rango vertical siempre incluye el umbral |
| Tras respirar, el cambio de emoción borraba al instante la frase de reconocimiento | Se mantiene un minuto |
| Demo: la explicación de la meta venía de la IA con la meta real (incoherente con la meta demo) | En demo se usa la frase local |
| Semana en tema claro: frase larga de la IA cortada con "…" | autoSize (10–13 sp) y margen de 12 dp |
| Simulador de pulso: lecturas con fecha hacia atrás quedaban desordenadas | Lecturas ordenadas y una por minuto (`agregarPulso`, con prueba) |

**Resultado:** pasa. Capturas en `docs/capturas/emulador/v1/`.
