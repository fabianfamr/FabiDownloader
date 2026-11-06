# FabiDownloader — Transplante del motor de Seal

La UI quedó **intacta** (44 de 45 archivos de `ui/` son byte a byte idénticos al
original; el único cambio es `AppSettings.kt`, que solo ganó dos propiedades
ocultas). Se reemplazó el núcleo de descarga por la arquitectura del motor de
Seal (JunkFood02) y se transplantó también su **sistema de notificaciones**,
conectando con la cola, la base de datos Room y las pantallas existentes.

## Mapa de estructura (Seal → FabiDownloader)

| Archivo de Seal | Nuevo archivo en FabiDownloader | Rol |
|---|---|---|
| `download/Task.kt` | `app/src/main/java/com/fabian/downloader/download/Task.kt` | Modelo de tarea + estados (Idle, Running, Canceled, Error, Completed) |
| `download/TaskFactory.kt` | `app/src/main/java/com/fabian/downloader/download/TaskFactory.kt` | Crea tareas y construye el `YoutubeDLRequest` (opciones del motor) |
| `Downloader.kt` / `DownloaderV2Impl` | `app/src/main/java/com/fabian/downloader/download/Downloader.kt` | Ejecución: progreso, cancelación por processId, reintentos limpios |
| `util/DownloadUtil.kt` | `app/src/main/java/com/fabian/downloader/util/DownloadUtil.kt` | Helpers: ordenador `-S`, aria2c, límite de velocidad, fragmentos |
| `util/NotificationUtil.kt` | `app/src/main/java/com/fabian/downloader/util/NotificationUtil.kt` | Sistema de notificaciones de Seal (ver abajo) |
| `NotificationActionReceiver.kt` | `receivers/DownloadActionReceiver.kt` (extendido) | Acciones Cancelar-tarea y Copiar-informe |
| `App.kt` (init de YoutubeDL+FFmpeg+Aria2c) | `MyApplication.kt` (modificado) | `Aria2c.getInstance().init()` en las 3 rutas de inicialización |
| `DownloadService.kt` | `services/DownloadForegroundService.kt` | El servicio en primer plano propio de Fabi sigue siendo el anfitrión; su notificación ahora se construye con `NotificationUtil.makeServiceNotification` (estructura de Seal) |

## Sistema de notificaciones transplantado (de Seal)

1. **`util/NotificationUtil.kt`** — el objeto de notificaciones de Seal,
   adaptado a los canales/IDs/iconos de Fabi:
   - `createNotificationChannel()`: **grupo de canales** "Descargas"
     (`NotificationChannelGroup`, como Seal) con los 3 canales de Fabi
     (progreso, completadas, fallidas) dentro del grupo.
   - `notifyProgress()`: progreso 0-100 con **botón "Cancelar"** que mata el
     proceso yt-dlp por taskId (`destroyProcessById`), como en Seal.
   - `notifyError()`: error con **acción "Copiar informe"** al portapapeles.
   - `makeServiceNotification()` / `updateServiceNotificationForQueue()`:
     notificación del servicio en primer plano con **contador de descargas
     activas** (el equivalente de `updateServiceNotificationForPlaylist`).
     Protegido para no sobreescribir el progreso de la primera descarga activa.

2. **Acciones sobre las notificaciones** (antes solo en Seal):
   - Notificación de **progreso** → botón **Cancelar** (cancela de verdad la
     descarga sin abrir la app).
   - Notificación de **pausa** → botones **Reanudar** (conserva el `.part`,
     reanuda con `--continue`) y **Cancelar**.
   - Notificación de **error** → botones **Reintentar** (ya existía) y
     **Copiar informe** (título + error al portapapeles, para reportar bugs).
   - Notificación de **completada** → **Abrir** y **Compartir** (ya existían).

3. **`receivers/DownloadActionReceiver.kt`** ahora también implementa el
   comportamiento del `NotificationActionReceiver` de Seal:
   - `ACTION_CANCEL_TASK` → destruye el proceso por taskId + borra la descarga.
   - `ACTION_ERROR_REPORT` → copia el informe al portapapeles + toast + cierra
     la notificación.
   - Registrados en `AndroidManifest.xml`.

4. **Strings en 6 idiomas** (es, en, de, fr, ja, ru):
   `notif_channel_group`, `notif_action_copy_report`, `notif_error_copied`,
   `notif_queue_count`.

## Archivos añadidos / modificados / eliminados

**Añadidos**
- `download/Task.kt`, `download/TaskFactory.kt`, `download/Downloader.kt`
- `util/DownloadUtil.kt` (helpers del motor)
- `util/NotificationUtil.kt` (notificaciones de Seal)

**Modificados**
- `gradle/libs.versions.toml` y `app/build.gradle.kts` → módulo
  `io.github.junkfood02.youtubedl-android:aria2c:0.18.1` (misma versión de
  `library`/`ffmpeg`; Seal usa el mismo módulo aria2c).
- `MyApplication.kt` → inicializa **Aria2c** junto a YoutubeDL y FFmpeg.
- `configs/Config.kt` → claves `PREF_USE_ARIA2C`, `PREF_USE_FAST_HTTP_STREAM`,
  constantes del motor y extras/acciones de notificación de Seal.
- `ui/AppSettings.kt` → `useAria2c` (activo por defecto) y
  `useFastHttpStream` (inactivo por defecto). Sin cambios de UI.
- `services/sites/BaseSiteService.kt` → todo sitio usa el nuevo motor;
  el "turbo directo" (SnapChunkDownloader) ahora es opcional.
- `services/NotificationService.kt` → grupo de canales + acciones
  Cancelar/Reanudar/Copiar-informe en las notificaciones existentes
  (mismos IDs, mismo texto base: la apariencia de la app no cambia).
- `services/DownloadQueueManager.kt` → actualiza el contador de cola en la
  notificación del servicio (como Seal).
- `services/DownloadForegroundService.kt` → notificación delegada en
  `NotificationUtil.makeServiceNotification`.
- `receivers/DownloadActionReceiver.kt` → acciones de Seal.
- `AndroidManifest.xml` → nuevas acciones del receptor.
- `values*/strings_downloads.xml` (6 idiomas) → 4 strings nuevos.

**Eliminados (motor viejo frágil)**
- `services/YtdlpDownloader.kt` (watchdog que mataba descargas sanas a los
  35–60 s + cascada de 4 niveles de `-f` + borrado de parciales).
- `services/YtdlpCommandBuilder.kt` (expresiones `-f` en cadena y sufijo de
  salida `.downloading`).

## Por qué ahora sí descarga

1. **aria2c como descargador externo** (`--downloader libaria2c.so`), el mismo
   que usa Seal: multiprotocolo, multiproceso y mucho más fiable que el
   descargador interno de Python en Android. Verificado: `libaria2c.so`
   queda empaquetado en el APK para las 5 ABI.
2. **Ordenador de formatos `-S`** en lugar de `-f bv*[height<=N]+ba/...`:
   `-S res:720,vcodec:h264` nunca "falla en cadena" si un formato no existe;
   yt-dlp elige el mejor disponible (igual que el preset de Seal).
3. **Plantilla de salida correcta**: `-o <carpeta>/<nombre>.%(ext)s` +
   `--continue`. Antes la salida era `<nombre>.downloading`, lo que producía
   archivos híbridos que el post-procesado no siempre resolvía. Los `.part`
   ahora reanudan descargas interrumpidas.
4. **Sin watchdog**: yt-dlp ya reintenta por sí mismo (`--retries`,
   `--fragment-retries`, `--socket-timeout`). El watchdog anterior abortaba
   procesos sanos mientras yt-dlp resolvía retos JS de YouTube.
5. **Turbo directo desactivado por defecto**: antes, para MP4/M4A, la app
   probaba primero el *primer* stream directo encontrado (que podía ser
   solo-audio o solo-video) y guardaba archivos defectuosos.
6. **Un único reintento nivelado** (si "autoRetry" está activo): nivel 0 =
   clientes por defecto de yt-dlp; nivel 1 = `youtube:player_client=ios,mweb`
   para casos de detección de bots. El auto-sanado (actualización silenciosa
   de yt-dlp ante errores de extractor) se conserva.

## Verificación realizada

- `gradlew compileDebugKotlin` → **BUILD SUCCESSFUL** (toda la UI compila
  contra el motor nuevo sin un solo cambio de pantalla).
- `gradlew assembleDebug` → **BUILD SUCCESSFUL**. APKs para arm64-v8a,
  armeabi-v7a, x86, x86_64 y universal. En el APK final:
  - `libaria2c.so` + `libaria2c.zip.so` empaquetados (motor de Seal).
  - `download/Downloader`, `download/TaskFactory`, `util/NotificationUtil`,
    `DownloadActionReceiver` presentes en el dex.
  - Manifest compilado con `ACTION_CANCEL_TASK` y `ACTION_ERROR_REPORT`.
- `gradlew testDebugUnitTest` → **32/32 tests OK**
  (pipeline `DownloadAssemblyLine`, DAO Room con Robolectric, PathUtils,
  VersionUtils).
- Diff completo contra el original: las pantallas y componentes de
  `ui/screens`, `ui/components`, `ui/theme` y `ui/viewmodels` están intactos.

## Cómo compilar y probar

```bash
./gradlew assembleDebug
# o release local: ./gradlew assembleRelease -PdebugBuild
```

1. Descargar un MP4 de YouTube (probar con "Mejor calidad" y con "720p").
2. Descargar un MP3 (bitrate por defecto o desde calidad).
3. Pausar y reanudar: el `.part` debe continuar desde donde quedó
   (también desde el botón "Reanudar" de la notificación de pausa).
4. Cancelar desde la notificación de progreso (nuevo, como Seal): no debe
   quedar proceso huérfano ni notificación colgada.
5. Probar un sitio no-YouTube (p. ej. TikTok/X) para validar
   `customizeDownloaderRequest`.
6. Al fallar una descarga: usar "Copiar informe" en la notificación de error.

## Ajustes ocultos (sin UI, para futuras iteraciones)

- `AppSettings.useAria2c` (SharedPreferences `useAria2c`, por defecto `true`).
- `AppSettings.useFastHttpStream` (por defecto `false`): reactiva el turbo
  directo solo para sitios con capacidad `FAST_HTTP_STREAM`.
