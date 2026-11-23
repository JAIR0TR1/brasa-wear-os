# Justificación de diseño — Brasa 1.0.0

Brasa es una interfaz para una pantalla redonda de unos 3 cm que se mira en un par de segundos, con el brazo levantado y a menudo en movimiento. Cada decisión parte de esa restricción: **entender el estado del día de un vistazo, sin leer cifras**.

## 1. Heurísticas de Nielsen

| Heurística | Cómo se aplica | Dónde verlo |
|---|---|---|
| **1. Visibilidad del estado del sistema** | La emoción de Brasa *es* el estado: color del fondo, postura y cara cambian con el progreso y la tensión. El anillo del borde muestra el % de la meta; la página activa se marca en el indicador inferior. | Inicio, indicador de páginas |
| **2. Relación con el mundo real** | Metáfora de fuego: los pasos son "leña", la constancia hace crecer la llama (Chispa → Llama → Fogata → Leyenda). Respirar es literal: Brasa se infla al inhalar y se desinfla al exhalar. | Evolución, Respirar |
| **3. Control y libertad** | "Ahora no" en permisos deja la app usable en modo manual. Deslizar a la derecha siempre vuelve atrás (gesto del sistema). La animación de subida de nivel se cierra sola o con un toque. | Permisos, Respirar, Subida de nivel |
| **4. Consistencia y estándares** | Navegación propia de Wear OS: páginas horizontales, corona para cambiar de página, deslizar para cerrar, títulos curvos en el borde. Todo color, tipo y espacio sale de `Tokens.kt`. | Toda la app |
| **5. Prevención de errores** | No se presenta un dato engañoso: si el pulso no bajó tras respirar, no se muestra como logro ("Un minuto para ti. Eso ya cuenta."). Con pocos datos de pulso se muestra "Midiendo tu pulso" en vez de una línea de dos puntos. | Resultado de respirar, Pulso |
| **6. Reconocer antes que recordar** | El botón Respirar aparece solo cuando hace falta (tensión), en el lugar de la frase; no hay que recordar dónde está. | Inicio inquieta |
| **7. Flexibilidad** | Tres formas de moverse: deslizar, corona y TalkBack. Modo demo para presentar sin sensores. Español/inglés y tema claro/oscuro. | Ajustes |
| **8. Diseño estético y minimalista** | Una cifra protagonista por pantalla, una frase corta (≤ 2 líneas) y nada más. El negro puro del fondo deja a Brasa como única fuente de luz. | Inicio, Gráficas |
| **9. Ayudar a reconocer y recuperarse de errores** | Sin red, sin sensores o sin permisos la app no muestra errores técnicos: cambia a frases locales o a modo manual ("Tócame para darme leña") y lo explica en Ajustes. | Inicio manual, Ajustes |
| **10. Ayuda y documentación** | La ayuda está en contexto: la pantalla de permisos explica para qué se usan los datos y que no es un dispositivo médico; los estados vacíos dicen qué hacer. | Permisos, estados vacíos |

## 2. Gestalt

- **Figura y fondo**: fondo negro puro (OLED apagado) y un degradado radial del color de la emoción detrás de Brasa. La figura no compite con nada.
- **Proximidad**: los pasos y su meta (`6.205 / 8.500`) van juntos en una línea; la frase de Brasa justo debajo, como si la dijera ella.
- **Semejanza**: un mismo color de acento por emoción en anillo, frase y botón; en la semana, las barras de días cumplidos comparten tono y las de no cumplidos son más tenues.
- **Continuidad y cierre**: el anillo de la meta sigue el borde del reloj y el ojo lo completa; en Pasos, las 24 barras forman un reloj de 24 h que se lee como una esfera.
- **Destino común**: en la subida de nivel las chispas salen todas del centro hacia fuera, y Brasa crece con ellas: se lee como un único acontecimiento.
- **Región común**: cada ajuste es una píldora con borde sutil; toda la píldora es táctil.

## 3. Ley de Fitts

En un reloj el dedo tapa buena parte de la pantalla, así que los objetivos deben ser **grandes y estar donde ya está el dedo**:
- Áreas táctiles de **al menos 48 × 48 dp** (el botón píldora mide como mínimo 96 × 48 dp; las filas de ajustes 52 dp de alto). Una prueba automática (`AccesibilidadTest`) mide todos los elementos táctiles de todas las pantallas en el reloj más pequeño (192 dp).
- **Brasa entera es el objetivo** de toque en Inicio (casi media pantalla).
- El botón **Respirar** aparece en el centro inferior, a la altura natural del pulgar, solo cuando hace falta.
- Navegar no exige apuntar: deslizar en cualquier punto o girar la corona.

## 4. WCAG 2.2 aplicadas a un reloj

| Criterio | Aplicación |
|---|---|
| 1.4.3 Contraste (AA) | Todos los colores de texto de los tokens tienen ≥ 4,5:1 sobre su fondo, en oscuro y en claro, incluidos los botones. Lo comprueba `AccesibilidadTest.contrasteAADeLosTextos`. |
| 1.4.1 Uso del color | Las emociones se distinguen por forma y gesto además de por color; el botón Respirar tiene icono y texto. |
| 1.4.4 / 1.4.12 Texto redimensionable | Todo el texto en `sp`. Se revisa con fuente ×1,3 en las capturas; las frases largas reducen su tamaño hasta un mínimo en lugar de cortarse. |
| 2.5.8 Tamaño del objetivo | ≥ 48 dp (por encima de los 24 px mínimos de WCAG; es la guía de Wear OS). |
| 2.3.3 Animación por interacción | Con "reducir animaciones" del sistema, Brasa queda quieta, las gráficas aparecen sin dibujarse y la subida de nivel se muestra estática el mismo tiempo. |
| 1.1.1 Contenido no textual | Brasa, las gráficas y el anillo tienen descripción para TalkBack ("Brasa, inquieta, etapa Llama"; "Pulso de hoy: reposo 55, máximo 96 latidos por minuto"). |
| 1.3.2 / 2.4.3 Orden de lectura | En Inicio: hora → Brasa → pasos ("6.205 pasos de 8.500") → frase o botón → anillo. Los títulos son encabezados. |
| 4.1.3 Mensajes de estado | Las fases de respirar y la subida de nivel se anuncian (regiones en vivo). |

## 5. Decisiones propias del reloj

- **Pantalla redonda**: títulos curvos en el borde, nada a menos de 18 dp del borde (`Tokens.Espacio.bordeSeguro`); en pantallas cuadradas el título pasa a ser recto. Se revisa cada pantalla en 6 variantes (redondo 227 dp y 192 dp, cuadrado 198 dp, claro, inglés y fuente grande).
- **Háptica con significado**: cada vibración comunica algo (ritmo de respiración, meta cumplida, subida de nivel) y respeta los ajustes del sistema.
- **Negro puro**: en OLED los píxeles negros no consumen batería; Brasa "brilla" de verdad.
- **Sin `Modifier.blur`**: todos los brillos son degradados radiales (más baratos y nítidos en el reloj).

## 6. Privacidad: por qué no hay cuentas propias

Una cuenta propia obligaría a guardar en un servidor quién es cada persona junto a sus datos de actividad, y añadiría un registro y una contraseña que en un reloj son muy incómodos de escribir. Brasa no lo necesita:
- Los **datos de salud exactos nunca salen del reloj**.
- El servidor solo recibe **niveles** (bajo/medio/alto) y un **id aleatorio** por reloj, que no identifica a nadie. El esquema es estricto: si llega un pulso exacto o texto libre, se rechaza.
- La IA no recibe ni el id.

## 7. Continuidad: la copia de seguridad de Google

Sin cuentas propias, ¿qué pasa al cambiar de reloj? Se usa la **copia de seguridad de Android** del propio usuario (cifrada, ligada a su cuenta de Google, que ya existe en el reloj). Se respalda **solo** un almacén separado con ajustes, XP total, respiraciones y meta. En el reloj nuevo Brasa vuelve con su nivel y su meta; el historial de pasos, el pulso y el id del reloj se quedan en el antiguo, y los permisos se vuelven a pedir porque son de cada dispositivo. Así se obtiene continuidad sin crear un sistema de cuentas ni mover datos sensibles.

## 8. No es un dispositivo médico

El texto nunca diagnostica: habla de "señales de tensión" y propone respirar. El aviso aparece en la pantalla de permisos y en Ajustes, y forma parte de las instrucciones de la IA.
