# Guion de la demo (5 minutos)

## Antes de empezar (5 min antes)
1. Abrir el emulador: `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Brasa_Wear_Redondo`
2. Instalar la versión entregada: `adb install -r entrega\Brasa-1.0.0.apk`
3. Idioma: `adb shell cmd locale set-app-locales com.brasa.app --locales es`
4. Comprobar el servidor en el navegador: https://brasa-servidor.vercel.app/api/salud → `"ok":true,"ia":true`
5. Abrir Brasa, pulsar "Ahora no" en permisos y en **Ajustes** activar **Modo demo** (datos de ejemplo marcados como demo).
6. Tener abierta una terminal en `C:\dev\brasa` para los comandos del simulador.

## Guion

**0:00 – 0:40 · El problema.** "Los relojes miden mucho y explican poco. Brasa traduce pasos y pulso a una emoción que se entiende de un vistazo." Mostrar Inicio: Brasa feliz, anillo de la meta en el borde, una cifra y una frase.

**0:40 – 1:40 · Emociones.** En Ajustes → Emoción, pasar por apagada, tranquila, feliz y eufórica. Decir: *"Se distinguen por forma, no solo por color: brazos caídos, saludo, brazos arriba."* Al llegar a eufórica, el reloj vibra (meta cumplida).

**1:40 – 2:40 · Tensión y respirar.** Elegir **inquieta**: aparece el botón Respirar en el lugar de la frase (solo cuando hace falta). Tocarlo, enseñar 10 s de Inhala/Exhala (Brasa se infla con la respiración) y explicar que el minuto completo termina con el pulso antes/después. Pulsar atrás deslizando a la derecha.

**2:40 – 3:30 · Gráficas y evolución.** Deslizar: Pasos (reloj de 24 h), Pulso (el pico de tensión y la respiración marcada), Semana (meta adaptativa), Evolución. En Ajustes → Etapa tocar tres veces (Chispa → Llama → Fogata): al subir a Llama y a Fogata sale la **animación de subida de nivel** con vibración (esperar ~3 s entre toques; tocar la animación la cierra).

**3:30 – 4:20 · Privacidad, IA y continuidad.** "Al servidor solo viajan niveles; si le mando un pulso exacto, lo rechaza":
```bash
curl -s https://brasa-servidor.vercel.app/api/mensaje -H "content-type: application/json" -d "{\"dispositivo\":\"3f6c2a1e-9b4d-4c7a-8e21-5d0b9a7f1c33\",\"idioma\":\"es\",\"situacion\":\"tension\",\"actividad\":\"medio\",\"emocion\":\"inquieta\",\"etapa\":\"llama\",\"pulso\":112}"
```
→ `400 entrada_invalida`. Quitar `,\"pulso\":112` → frase de Gemini. Cambiar de reloj: "se usa la copia de seguridad de Google: vuelve el nivel, no los datos de salud".

**4:20 – 5:00 · Diseño.** Tema claro (Ajustes → Tema), fuente grande y lector de pantalla: contraste AA probado automáticamente, áreas táctiles de 48 dp, nada a menos de 18 dp del borde, 170 + 17 pruebas.

## Mostrar con datos "reales" (simulador)
Con el modo demo **desactivado**:
```bash
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 2500   # tranquila
adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 9000   # eufórica (+ vibración; puede subir de nivel)
```
Tensión: solo si el reloj no está recibiendo pasos. Revocar el permiso de pasos (`adb shell pm revoke com.brasa.app android.permission.ACTIVITY_RECOGNITION`), reabrir la app, enviar `--ei pulso 112`, esperar 70 s y enviarlo otra vez.

## Dos relojes a la vez
Cerrar el emulador y abrir dos instancias del mismo AVD:
```bash
emulator -avd Brasa_Wear_Redondo -read-only -port 5554
emulator -avd Brasa_Wear_Redondo -read-only -port 5556
adb -s emulator-5554 install -r entrega\Brasa-1.0.0.apk
adb -s emulator-5556 install -r entrega\Brasa-1.0.0.apk
adb -s emulator-5554 shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 6500
```
Cada reloj tiene su propio id anónimo, su estado y sus frases.

## Restauración al cambiar de reloj (en el emulador)
```bash
adb shell bmgr enable true
adb shell bmgr transport com.android.localtransport/.LocalTransport
adb shell bmgr backupnow com.brasa.app
adb shell pm clear com.brasa.app          # "reloj nuevo"
adb shell bmgr restore 1 com.brasa.app
```
Abrir Brasa: pide permisos otra vez (son de cada reloj) y en Evolución aparece el mismo nivel. Al terminar: `adb shell bmgr transport com.google.android.gms/.backup.BackupTransportService`.

## Plan B sin internet
La app no depende de la red: activar el modo avión del emulador (`adb shell cmd connectivity airplane-mode enable`) y repetir el guion; Brasa usa sus frases locales sin errores ni esperas. Si el emulador falla, usar las capturas de `docs/screenshots/` y `docs/capturas/emulador/v1/`.

## Respuestas cortas
- **¿Por qué no tiene cuentas?** Porque no le hacen falta: los datos de salud se quedan en el reloj y al servidor solo van niveles con un id aleatorio. Una cuenta obligaría a guardar quién eres junto a tu actividad y a escribir una contraseña en un reloj.
- **¿Qué pasa si cambio de reloj?** La copia de seguridad de Google de tu cuenta restaura los ajustes y el progreso (nivel, XP, meta). El historial de salud no viaja, y los permisos se vuelven a pedir.
- **¿Es un dispositivo médico?** No. Lee señales de tensión (pulso alto estando quieta) y propone respirar; no diagnostica nada, y lo dice en la pantalla de permisos y en Ajustes.
