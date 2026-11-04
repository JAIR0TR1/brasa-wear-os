# Dirección de arte — Brasa

## Concepto: "fuego vivo en la oscuridad"
Brasa es la **única fuente de luz** de la pantalla. En tema oscuro el fondo es negro puro (#000000): en OLED esos píxeles están apagados, así que la criatura literalmente brilla y la batería lo agradece. Cada emoción tiñe el centro con un degradado radial que se desvanece a negro antes del borde.

Referencias de partida: `docs/referencias/` (lienzos de arranque, emociones, evolución y gráficas). Se conservan su paleta, la silueta de gota y el fondo radial. Mejoras sobre ellas:
- **Brasa tiene brazos y piernas** (las referencias eran una gota con cara): es un personaje, no un ícono.
- Las llamas laterales de Fogata/Leyenda se animan con vaivén y la corona es de fuego (tres lenguas doradas), no un triángulo.
- Textos de datos más grandes y cortos que en las referencias (legibles a la distancia de la muñeca).
- Ningún color fuera de los tokens (el chip azul de "−14 lpm" pasa a la paleta propia).

## Tokens (`ui/theme/Tokens.kt`, única fuente de verdad)
| Rol | Oscuro | Claro |
|---|---|---|
| Fondo | #000000 | #FFF8F0 (blanco cálido) |
| Texto | #FFF3E6 | #2A1408 |
| Texto secundario | #B9A89A | #6B5444 |
| Fuego (acento) | #FF8A1F | #C2410C |
| Fuego claro / profundo | #FFF4C2 / #D92B12 | — |
| Magenta / Violeta / Dorado | #FF2E88 / #7A1CFF / #FFD27A | — |

Emociones (cuerpo · núcleo · borde · acento oscuro/claro):
| Emoción | Base | Núcleo | Borde | Acento oscuro | Acento claro |
|---|---|---|---|---|---|
| Apagada | #7F93B8 | #DCE4F2 | #3E4C6B | #9FB2D6 | #41557D |
| Inquieta | #FF5A4E | #FFC4B4 | #A3133A | #FF7A70 | #B8153A |
| Tranquila | #FF9E45 | #FFE6BC | #C25A12 | #FFB46E | #A34C0C |
| Feliz | #FF8A1F | #FFF4C2 | #D92B12 | #FFA14D | #B83A0A |
| Eufórica | #FF4F93 | #FFE7A8 | #B0105A | #FF79AE | #B0105A |

Los acentos claros están elegidos para contraste ≥ 4.5:1 sobre #FFF8F0 (WCAG AA texto normal); los oscuros, ≥ 4.5:1 sobre negro.

## Tipografía (empaquetada, licencia OFL en `docs/licencias/`)
- **Space Grotesk** (variable, 500/700): cifras y títulos. Geométrica y técnica: los números "se sienten" de reloj.
- **Fraunces itálica** (variable, SOFT 100): la voz de Brasa. Una serif cálida y blanda contrasta con los datos y hace que las frases suenen a personaje.
- **JetBrains Mono**: datos pequeños (reglas, horas, niveles). Monoespaciada = cifras alineadas.
Todo en `sp` para respetar el tamaño de fuente del sistema.

## La mascota: anatomía por capas (Canvas, sin blur)
1. **Halo** — degradado radial del color de la emoción (luz propia).
2. **Sombra** — elipse en el suelo que se encoge cuando Brasa sube (vende la flotación).
3. **Llamas laterales** — Fogata y Leyenda; vaivén independiente.
4. **Piernas** — cápsulas cortas con pie ovalado, detrás del cuerpo; patalean suave al flotar y se recogen al saltar.
5. **Cuerpo** — gota de fuego: degradado radial núcleo → base → borde con el centro de luz arriba a la izquierda; la punta oscila (vaivén + parpadeo de llama); una llama interior más clara enciende la punta sin tocar la cara; brillo especular ovalado arriba a la izquierda.
6. **Brazos** — "manguera" curva (Bézier cuadrática) con contorno y mano redonda; giran desde el hombro. Son el canal más expresivo del cuerpo.
7. **Cara** — ojos ovalados con reflejos, párpado caído recortado con `Path.op`, ojos felices "^ ^", cejas de preocupación, boca (línea, abierta con lengua, u ondulada), mejillas.
8. **Agua** — lágrima que cae (apagada) y gota de sudor (inquieta).
9. **Celebración** — chispas de 4 puntas (eufórica), corona de fuego y chispas en órbita (Leyenda).

### Emociones (se distinguen por color **y** por forma: nunca solo por color)
| Emoción | Cuerpo | Brazos | Cara | Ritmo |
|---|---|---|---|---|
| Apagada | pequeña, azul grisácea, llama baja | colgando | párpados caídos, boca triste, lágrima | flota lento |
| Inquieta | rojo-magenta, tiembla | manos a la boca ("se muerde") | ojos enormes, cejas preocupadas, boca ondulada, sudor | rápido y nervioso |
| Tranquila | naranja suave | relajados, vaivén lento | sonrisa leve | se balancea despacio |
| Feliz | naranja brillante | **saluda** con la derecha | ojos grandes, boca abierta, mejillas | flota alegre |
| Eufórica | rosa con núcleo dorado | ambos arriba | ojos "^ ^", boca abierta | **salta**, chispas |

### Evolución
| Etapa | Nivel | Forma |
|---|---|---|
| Chispa | 1–4 | esfera pequeña con una punta mínima, extremidades cortas |
| Llama | 5–9 | gota completa |
| Fogata | 10–19 | más grande, dos llamas laterales |
| Leyenda | 20+ | corona de fuego dorada y chispas en órbita |

## Movimiento
- **Base**: respiración (escala vertical ±3 %), parpadeo aleatorio cada 3–6 s, punta de llama que oscila, brazos que se balancean.
- **Cambio de emoción**: Brasa "sale y entra" con un rebote (0.84 → spring 1.0) mientras toda la pose se interpola 520 ms; el fondo cambia de color en 600 ms.
- **Subida de etapa**: crece (1.15) y asienta con spring; la forma se interpola 900 ms.
- **Toque**: rebote elástico + risa (ojos "^ ^", boca abierta) + vibración.
- Se anima solo transformación, opacidad y color; el tiempo se lee en la fase de dibujo (sin recomponer).
- Se pausa cuando la actividad no está en primer plano; con "reducir animaciones" del sistema Brasa queda estática y los cambios son directos.

## Pantalla redonda
- Texto curvo arriba siguiendo el borde (`CurvedLayout`).
- Indicadores y anillos abrazan el borde; contenido dentro del círculo inscrito con márgenes de 24 dp.
- Textos de una línea con elipsis; probado en 192 dp, 227 dp y cuadrado 198 dp.
