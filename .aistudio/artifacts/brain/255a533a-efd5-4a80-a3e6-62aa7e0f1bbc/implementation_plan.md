# Plan de Optimización de Alto Rendimiento y Aceleración de Descargas

Plan integral para maximizar la velocidad de descarga, optimizar el uso de CPU/RAM/batería y acelerar el procesamiento de medios, integrando las mejores prácticas de proyectos líderes de código abierto en GitHub (Seal, YTDL-nis y NewPipe).

> [!IMPORTANT]
> **Decisiones Críticas Confirmadas por el Usuario (Fase 1):**
> - **Estrategia de aceleración**: Fragmentos paralelos nativos de `yt-dlp` (`--concurrent-fragments` adaptativo de 4 a 8 hilos de red) y habilitación de archivos `.part` con reanudación (`--continue`), eliminando re-descargas completas ante cortes de red.
> - **Concurrencia de cola**: Asignación dinámica basada en hardware (`Runtime.availableProcessors()` y memoria RAM disponible) con limitador automático bajo batería baja.
> - **Post-procesamiento de medios**: Copia directa de flujos (*stream copy* / remuxing sin re-codificación) en lugar de transcodificaciones lentas por CPU en Android, reduciendo el tiempo de finalización de minutos a milisegundos.

---

## 1. Overview & Core Concept

### Qué resuelve este plan
Actualmente, el proceso de descarga sufre de tres cuellos de botella clave identificados en el código:
1. **Límites rígidos en fragmentos**: Se restringían los fragmentos paralelos a un máximo de 3 y se deshabilitaba la reanudación con `--no-part`, lo que causaba descargas lentas en redes rápidas y pérdida total del progreso ante desconexiones.
2. **Re-codificación innecesaria por CPU**: `postProcessFile` ejecutaba conversiones completas de escala de video con FFmpeg si la resolución difería ligeramente, saturando el procesador del dispositivo móvil durante minutos.
3. **Colas estáticas**: La cola de descargas no medía dinámicamente la memoria ni los núcleos disponibles del dispositivo para balancear el paralelismo.

### Valor para el usuario
- **Velocidad multiplicada (hasta 3-4x más rápida)**: Descarga simultánea de fragmentos de audio/video y chunks HTTP optimizados para evitar el estrangulamiento de los servidores de contenido.
- **Tolerancia a fallos y reanudación instantánea**: Si la conexión se interrumpe al 80%, la descarga se reanuda desde ese punto exacto sin perder datos.
- **Post-procesamiento casi instantáneo**: El archivo pasa de "descargando" a "completado" en menos de un segundo gracias al ensamblado directo por remuxing sin recodificar.
- **Uso eficiente de batería y memoria**: La aplicación ajusta su agresividad según la gama del dispositivo, los núcleos disponibles y el estado de la batería.

---

## 2. Experiencia de Usuario y Ajustes Visuales

### Flujos de Usuario Principales
1. **Descarga Rápida con Reanudación Transparente**: El usuario inicia una descarga; la barra de progreso refleja la velocidad en tiempo real con indicador dinámico de fragmentos activos y ancho de banda aprovechado. Si hay una pérdida momentánea de señal, el estado pasa a pausa/reintento y se reanuda automáticamente en el punto alcanzado.
2. **Gestión Dinámica de Concurrencia**: Al encolar múltiples elementos, el sistema activa automáticamente el número óptimo de descargas simultáneas (por ejemplo, 3 en dispositivos octa-core con 6GB+ RAM, o 1-2 en dispositivos más modestos), manteniendo el teléfono fluido y sin sobrecalentamiento.
3. **Indicador de Post-Procesamiento Ultrarrápido**: La fase de empaquetado final ya no congela la interfaz con tiempos de espera prolongados; la notificación finaliza de forma inmediata con feedback háptico y visual claro.

### Controles en Pantalla de Ajustes
- **Selector de Modo de Descarga**:
  - *Modo Turbo* (Concurrencia alta de 6-8 fragmentos en Wi-Fi/5G).
  - *Modo Adaptativo Inteligente* (Equilibrio automático según CPU/batería - Recomendado).
  - *Modo Ahorro de Datos/Batería* (Fragmentación mínima, preservación de batería).
- **Indicador de Rendimiento del Hardware**: Vista informativa que detecta núcleos de CPU, RAM disponible y perfil calibrado para el dispositivo.

---

## 3. Decisiones Técnicas y Comparativa con GitHub

### Decisión 1: Fragmentación Nativa vs. Aria2c
- **Enfoque Elegido**: Fragmentación paralela nativa de `yt-dlp` (`--concurrent-fragments 4..8`) con reanudación por `.part` y `--continue`.
- **Por qué**: Como demuestran proyectos como Seal y YTDL-nis, `yt-dlp` en sus versiones modernas maneja streams HLS y DASH en paralelo con un rendimiento comparable a aria2c en Android, pero sin requerir binarios externos adicionales, enlaces de arquitectura C/C++ ni sobrecargar la inicialización de procesos en el sistema.
- **Alternativas descartadas**: Incorporar un binario compilado de aria2c agrega peso al APK, complejidad de permisos NDK y consumo adicional de descriptores de archivos.

### Decisión 2: Remuxing Directo (`-c copy`) frente a Transcodificación
- **Enfoque Elegido**: Empaquetado por contenedor (`--merge-output-format mp4/mkv`) sin filtros de re-escalado FFmpeg a nivel de CPU.
- **Por qué**: En smartphones, la transcodificación por software con FFmpeg de videos 1080p o 4K genera calor extremo, drenaje severo de batería y bloqueos por Out-Of-Memory (OOM). Las aplicaciones de referencia en GitHub descargan el flujo de video y audio en el mejor contenedor compatible y sólo realizan remuxing directo (`copy`), reservando la recodificación exclusivamente a la herramienta manual de conversión si el usuario la solicita explícitamente.
- **Alternativas descartadas**: Re-escalar video automáticamente ante diferencias menores de resolución.

### Decisión 3: Asignación Dinámica de Recursos de Cola
- **Enfoque Elegido**: Un evaluador dinámico en tiempo de ejecución que calcula `maxConcurrentDownloads` en base a:
  - Núcleos activos: `Runtime.getRuntime().availableProcessors()`
  - Límite de memoria heap de la JVM: `Runtime.getRuntime().maxMemory()`
  - Estado del gestor térmico y de batería (`BatteryOptimizerManager`)
- **Por qué**: Evita el problema común de que varios procesos concurrentes de Python/yt-dlp compitan por el ancho de banda del bus de memoria y el almacenamiento flash en dispositivos de gama media/baja, mientras desata toda la potencia en terminales de gama alta.

---

## 4. Arquitectura del Sistema y Flujo de Datos

```
 ┌─────────────────────────────────────────────────────────────┐
 │                      Capa de Usuario                        │
 │   URL Input / Compartir  ──▶  DownloadsScreen / Settings   │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                 Hardware Resource Profiler                  │
 │   • CPU Cores   • Max Heap RAM   • Estado Térmico/Batería   │
 └──────────────────────────────┬──────────────────────────────┘
                                │ Ajusta cupos y fragmentos
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                 DownloadQueueManager Dinámico               │
 │   • Semáforo dinámico de concurrencia                       │
 │   • Priorización de descargas forzadas vs. normales         │
 └──────────────────────────────┬──────────────────────────────┘
                                │ Dispara tareas
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                 YtdlpCommandBuilder Optimizado              │
 │   • --concurrent-fragments (4 a 8 adaptativo)               │
 │   • Archivos .part + --continue (Reanudación total)         │
 │   • --http-chunk-size 10M & buffer tuning                   │
 │   • Selección de flujos nativos sin re-codificación         │
 └──────────────────────────────┬──────────────────────────────┘
                                │ Ejecución con persistencia
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                     Stream Remuxer                          │
 │   • Fusión MP4/MKV directa (-c copy)                        │
 │   • Extracción de audio lossless / stream nativo            │
 │   • Finalización instantánea en disco y BD                  │
 └─────────────────────────────────────────────────────────────┘
```

### Mapeo de Componentes a Modificar en la Ejecución
1. **`YtdlpCommandBuilder`**:
   - Eliminar el bloqueo forzado de fragmentos en 3 (`coerceIn(1, 3)`), permitiendo de 1 a 8 según el perfil del hardware y ajustes del usuario.
   - Reemplazar `--no-part` por la gestión de archivos temporales `.part` y `--continue`, garantizando reanudación transparente.
   - Ajustar `--http-chunk-size` a 10M de forma segura para mitigar la limitación de velocidad impuesta por servidores de video.
2. **`DeviceOptimizationHelper`**:
   - Implementar cálculo dinámico del perfil de hardware (`HardwareTier`: Low, Mid, High, Ultra) evaluando núcleos y memoria libre.
   - Configurar tamaños de buffer y fragmentos óptimos por nivel.
3. **`DownloadQueueManager`**:
   - Calcular la capacidad de paralelismo en base al perfil de hardware y la batería, evitando saturación del sistema.
4. **`DownloadExecutor`**:
   - Optimizar `postProcessFile`: suprimir la re-codificación automática de video por resolución cuando el formato descargado ya sea compatible, utilizando remuxing directo ultrarrápido.
5. **`AppSettings` y UI de Ajustes**:
   - Exponer opciones actualizadas para fragmentación paralela (1, 2, 4, 6, 8, Automático) y modo de aceleración en la interfaz.

---

## 5. Plan de Verificación y Pruebas

- **Pruebas de Carga de Cola**: Validar que la asignación dinámica de slots no supere el umbral seguro de memoria en dispositivos con recursos limitados.
- **Pruebas de Reanudación de Descargas**: Simular interrupción forzada de red durante la descarga y verificar que la reanudación con `--continue` parte del porcentaje previo sin reiniciar desde cero.
- **Pruebas de Post-Procesado Instantáneo**: Comprobar que videos descargados en MP4/MKV se entregan en menos de 1 segundo tras finalizar los fragmentos sin entrar en transcodificación por FFmpeg.
- **Verificación de Compilación**: Ejecutar `compile_applet` para confirmar la ausencia de errores de sintaxis, imports o tipos en todo el proyecto.
