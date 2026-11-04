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
