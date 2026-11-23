# Brasa — ficha para el portafolio

Todo lo que aparece aquí está verificado en el repositorio (23-nov-2026).

## Título
**Brasa** — mascota de fuego para Wear OS que traduce pasos y pulso en emociones.

## Descripción corta (1 línea)
App para relojes Wear OS en la que una pequeña criatura de fuego lee tus pasos y tu pulso y te dice, con su emoción, si toca moverte, calmarte o descansar.

## Descripción larga
Brasa es una app independiente para Wear OS hecha en Kotlin y Compose. En lugar de anillos y porcentajes, muestra una mascota que cambia de emoción según tu día: se apaga si llevas horas sin moverte, se pone feliz cuando avanzas y celebra cuando cumples la meta. Si detecta una señal de tensión (pulso alto estando en reposo) te propone respirar un minuto con ella, inflándose y desinflándose al ritmo de la respiración. Crece contigo en cuatro etapas, tiene gráficas diseñadas para una pantalla redonda y habla con frases generadas por Gemini a partir de datos agregados, sin que tus pasos ni tu pulso exactos salgan nunca del reloj.

## Problema
Los relojes inteligentes muestran muchos datos que cuesta interpretar en una pantalla de 3 cm, y no distinguen un pulso alto por ejercicio de uno por tensión.

## Mi papel
Diseño de interfaz y de interacción, dirección de arte, ilustración procedural de la mascota (Canvas), desarrollo Android/Wear OS, servidor y pruebas.

## Funciones
- 5 emociones que se distinguen por forma y gesto, no solo por color.
- Detección de señales de tensión (pulso alto en reposo) y respiración guiada de 1 minuto con háptica.
- Gráficas de pasos por hora (radial), pulso del día y semana contra una meta adaptativa.
- Evolución en 4 etapas con animación de subida de nivel.
- Frases con IA (Gemini Flash-Lite) y respaldo local: funciona sin internet.
- Español e inglés, tema oscuro y claro, modo demo.
- Continuidad al cambiar de reloj con la copia de seguridad de Google, sin cuentas propias.

## Tecnologías
Kotlin 2.3.20 · Jetpack Compose (BOM 2026.03.01) · Compose for Wear OS Material 3 1.7.1 · Health Services 1.1.0 · DataStore 1.3.0 · kotlinx.serialization · TypeScript · Vercel Functions · Gemini (`@google/genai` 2.32.0) · Zod · Roborazzi + Robolectric · Compose UI Test.

## Cifras verificables
- **187 pruebas automáticas** en verde: 170 en la app (23 de reglas y datos, 8 de interfaz, 5 de accesibilidad, 6 de funcionamiento sin servidor y 128 capturas de pantalla) + 17 del servidor.
- **128 capturas** comparadas en cada cambio, en 6 variantes: redondo 227 dp, redondo 192 dp, cuadrado 198 dp, tema claro, inglés y fuente ×1,3.
- Contraste **AA (≥ 4,5:1)** en todos los colores de texto, comprobado por una prueba.
- Áreas táctiles **≥ 48 dp** en todas las pantallas, comprobadas por una prueba.
- 2 idiomas, 2 temas, 6 páginas principales, 5 emociones, 4 etapas.

## Enlaces
- Código: https://github.com/JAIR0TR1/brasa-wear-os (rama `mejoras-v1`)
- Servidor: https://brasa-servidor.vercel.app/api/salud
- Justificación de diseño: `docs/justificacion-diseno.md`

## Mejores capturas (en `docs/screenshots/`)
1. `01-inicio.png` — Inicio: Brasa feliz, anillo de la meta y frase.
2. `02-emociones.png` — las 5 emociones.
3. `03-respirar.png` — respiración guiada y resultado.
4. `04-graficas.png` — pasos, pulso y semana.
5. `05-evolucion.png` — Chispa, Llama, Fogata y Leyenda.
6. `06-tema-claro.png` — tema claro.
7. `07-subida-nivel.png` — animación de subida de nivel.

## Estado
Versión 1.0.0 terminada (noviembre de 2026). Pendiente para una versión futura: resumen del día con IA, lectura con la app cerrada, almacenamiento persistente en el servidor y tile de Wear OS.
