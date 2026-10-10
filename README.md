# Ritmo

Aplicación Android local de pasos **simulados**, en Kotlin, Jetpack Compose y Material 3. No usa Internet, cuentas, sensores, Google Fit, Health Connect ni permisos de actividad. Android 8.0 (API 26) o superior.

## Abrir y ejecutar

Abre esta carpeta en Android Studio, instala Android SDK 35 y sincroniza Gradle. Selecciona un emulador o dispositivo API 26+ y ejecuta `app`. Se requieren JDK 17 o 21 y conexión para descargar herramientas/dependencias durante la compilación; la aplicación funciona sin conexión.

```sh
./gradlew testDebugUnitTest assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Simulación

El hash SHA-256 de la fecha ISO genera una semilla estable, independiente de zona horaria y versiones de Kotlin. Los objetivos son 7,800–9,800 entre semana y 3,800–7,200 en fines de semana. La meta visual es siempre 7,500. Una interpolación suave por tramos con pequeñas variaciones deterministas y monótonas alcanza el objetivo a las 23:45. Fechas pasadas muestran su objetivo y futuras cero.

El repositorio conserva localmente el máximo del día para resistir cambios hacia atrás del reloj y reinicios. No hay servicio permanente: el cálculo se actualiza al volver a primer plano y cada 60 segundos mientras la actividad está visible. Las semanas comienzan en domingo; promedios incluyen solo los días transcurridos. Las gráficas muestran días futuros como cero.

## Depuración

Toca «Pasos» siete veces. Selecciona fecha y hora para consultar objetivo y pasos del momento simulado. No modifica los datos del panel.

## Estructura

`StepSimulationEngine` contiene la lógica pura; `StepRepository` guarda el máximo diario; `MainViewModel` expone la hora. `DayScreen`, `WeekScreen`, `MonthScreen` y los componentes de gráfica son Compose. Las pruebas cubren rangos, determinismo, monotonía durante un año, límites y reglas temporales.

La paleta sigue los colores solicitados. No se adjuntó una captura de referencia, por lo que el diseño se basa en la jerarquía y especificaciones del pedido.

## Validación en este entorno

Las herramientas del entorno están en `/workspace/android-sdk`. Para repetir las comprobaciones en esta máquina:

```sh
export ANDROID_USER_HOME=/workspace/tooling/android-user
export GRADLE_USER_HOME=/workspace/tooling/gradle-home
./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

No se necesita ningún servidor para ejecutar la aplicación. En Android Studio, usa el SDK de tu propia máquina; `local.properties` es local y no forma parte del proyecto distribuido.
