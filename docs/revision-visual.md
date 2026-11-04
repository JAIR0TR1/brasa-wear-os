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
