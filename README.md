# Ritmo 2.0

Aplicación Android en Kotlin, Jetpack Compose y Material 3, con paneles oscuros de actividad diaria, semanal y mensual. Android 8.0 (API 26) o superior. Funciona sin conexión, cuentas, sensores, permisos de actividad, Google Fit ni Health Connect.

## Instalar y ejecutar

Abre esta carpeta en Android Studio, instala Android SDK 35 y sincroniza Gradle. Selecciona un emulador o dispositivo API 26+ y ejecuta `app`. Para actualizar una instalación anterior, usa el APK con la misma firma; la versión 2 migra el progreso local anterior.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Usa JDK 17 o 21. Solo la descarga de herramientas y dependencias durante la compilación requiere conexión. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Actividad e historial

- El historial comienza el **1 de septiembre de 2026**, incluso si es la primera vez que se instala la app. Se calcula localmente y no depende de haber abierto la aplicación esos días.
- Lunes a viernes: **7,800–9,800** pasos al terminar el día.
- Sábado y domingo: **7,000–8,200** pasos al terminar el día.
- La meta visual se mantiene en **7,500**. Todos los días laborables completos la alcanzan; algunos fines de semana quedan entre 7,000 y 7,499.
- Los días pasados muestran su total final, hoy muestra progreso según la hora local y los días futuros muestran cero. Los periodos anteriores al inicio del historial no tienen actividad.
- El hash SHA-256 de cada fecha ISO produce un objetivo estable. Una curva suave por tramos, con pequeñas variaciones monótonas, alcanza el total a las 23:45.
- Se conserva el máximo **de cada fecha** para que no disminuya al atrasar el reloj, cambiar de fecha o reiniciar la app. El almacenamiento de la versión anterior se migra sin borrar su máximo.
- El cálculo se actualiza al volver al primer plano y cada 60 segundos mientras está visible. No hay servicios permanentes ni permisos especiales.

Los pasos son datos sintéticos, no mediciones reales. La explicación está en **Más opciones → Acerca de Ritmo** y en el laboratorio; no se repite en los paneles.

## Navegación

Las flechas cambian de día, semana o mes. El botón `+` y el título de la fecha abren el calendario, limitado al historial disponible. Toca una barra o una fila para abrir el día y vuelve a la lista con Atrás. Las semanas comienzan en domingo. Los promedios incluyen solo los días con historial hasta hoy, sin contar fechas futuras.

Cada pestaña tiene un acceso a **Todo tu historial**, también disponible en el menú. Allí están todos los días desde septiembre agrupados por mes. **Ir a hoy** restablece el panel actual. Las metas alcanzadas se marcan con un círculo y una palomita azul.

## Laboratorio

Toca «Pasos» siete veces seguidas. Elige fecha y hora para consultar el objetivo y el progreso correspondiente, sin alterar los datos del panel.

## Arquitectura y pruebas

`StepSimulationEngine` calcula objetivos y curvas puras. `StepHistory` reúne las reglas de fechas y persistencia con un almacén sustituible para pruebas. `StepRepository` adapta SharedPreferences. `MainViewModel` expone la hora. Las tres pantallas, el historial y los componentes de gráfica usan Compose.

Las 21 pruebas unitarias cubren rangos, determinismo, cada minuto de un año, límites, historia desde septiembre, año nuevo, año bisiesto, cambios de reloj, reinicios y migración. Android Lint y la compilación del APK se ejecutan con Gradle. El workflow de GitHub Actions genera el artefacto `Ritmo-debug-apk` al actualizar `main` o iniciarse manualmente.

## Entorno en la nube

El checkout ya está aislado; no hace falta crear otro worktree. Las herramientas están en `/workspace/android-sdk`. Para repetir las comprobaciones aquí:

```sh
export ANDROID_USER_HOME=/workspace/tooling/android-user
export GRADLE_USER_HOME=/workspace/tooling/gradle-home
./gradlew testDebugUnitTest assembleDebug lintDebug --no-daemon
```

En Android Studio, usa el SDK de tu propia máquina. `local.properties`, las cachés y las salidas de compilación son locales y están excluidos de Git.
