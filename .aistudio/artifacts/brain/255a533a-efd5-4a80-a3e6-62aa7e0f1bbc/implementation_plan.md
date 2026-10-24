# Plan de Auditoría Exhaustiva y Resolución Modular de Errores

Plan estratégico de diagnóstico estático profundo y resolución sistemática de errores críticos de estabilidad, fallos de descarga, pérdidas de concurrencia y anomalías de ciclo de vida en toda la aplicación, organizados en cuatro subsistemas lógicos independientes.

> [!IMPORTANT]
> **Decisiones Clave Confirmadas por el Usuario (Fase 1):**
> - **Prioridad de Errores**: Errores críticos de estabilidad, fallos en descargas y caídas de la aplicación (evitando bloqueos en hilos de ejecución o paradas inesperadas).
> - **Método de Diagnóstico**: Análisis estático profundo y revisión modular de código fuente, examinando contratos de tipos, concurrencia, gestión de corrutinas y casos límite.
> - **Estrategia de Ejecución**: Agrupación y aplicación de correcciones por subsistema funcional en bloques lógicos independientes y verificables.

---

## 1. Overview & Core Concept

### Objetivo de la Auditoría
Garantizar la estabilidad absoluta de la aplicación en condiciones reales de uso (conexiones inestables, cambio de redes, suspensión de procesos en segundo plano, cancelación/pausa rápida de tareas y variaciones de hardware en Android 10 a 14).

### Grupos de Trabajo (Subsistemas)
1. **Grupo 1: Motor de Descarga y Pipeline de Ejecución (`services/`, `pipeline/`)**: Resolución de estados inconsistentes entre tareas en cola, sincronización de mutexes, reanudación limpia sin corrupción de títulos y gestión de procesos del motor `yt-dlp`.
2. **Grupo 2: Red, Conectividad y Extracción de Medios (`network/`, `services/sites/`)**: Validación de conectividad heterogénea (datos móviles, Wi-Fi, portales cautivos), timeouts estrictos en extracción para evitar colas bloqueadas y robustez en peticiones de metadatos.
3. **Grupo 3: Almacenamiento, Archivos y Persistencia Room (`database/`, `utils/PathUtils`)**: Prevención de excepciones por Scoped Storage en Android 13/14, verificación segura de espacio en disco, transacciones atómicas en base de datos y limpieza de archivos temporales huérfanos.
4. **Grupo 4: Interfaz de Usuario, Ciclo de Vida y Notificaciones (`ui/`, `receivers/`, `workers/`)**: Protección contra navegación prematura en Compose, sincronización de `LiveProgress` sin saturar el hilo principal, gestión del receptor de compartir (`ShareReceiverActivity`) y cancelación de canales de notificación.

---

## 2. Experiencia de Usuario y Estabilidad Visual

### Impacto Directo para el Usuario
- **Descargas a Prueba de Fallos**: Las descargas ya no se quedarán congeladas en estados infinitos de "Conectando" o "Calculando". En caso de error externo real (bloqueo del servidor o enlace inválido), la aplicación proporciona un diagnóstico claro y recuperable con un solo toque.
- **Transiciones y Navegación Suaves**: Eliminación de bloqueos de fotogramas (*jank*) al desplazarse por listas grandes de descargas activas, aislando el cálculo de progreso en hilos secundarios.
- **Consistencia Visual en Títulos y Estados**: Eliminación de acumulaciones repetitivas de prefijos como `[FALLIDO]` en los títulos al reintentar, preservando siempre el nombre limpio del contenido multimedia.

---

## 3. Decisiones Técnicas y Plan de Resolución por Grupos

### Grupo 1: Motor de Descargas y Pipeline (Prioridad Alta)
- **Problema Detectado**: En `DownloadManagerService` y `DownloadActionReceiver`, la reanudación y el reencolado de tareas fallidas utilizaban manipulación de cadenas con `substringAfter(STATUS_FAILED_PREFIX)`, lo que eliminaba partes del título legítimo si contenía caracteres coincidentes. Asimismo, cuando una tarea fallaba antes de iniciar el hilo de trabajo, el estado en la base de datos podía quedar desincronizado del semáforo de cola.
- **Acción Correctiva**:
  - Reemplazar toda manipulación de títulos con desinfección pura `removePrefix().trim()`.
  - Asegurar que cualquier fallo durante `startDownload` libere inmediatamente cualquier id retenido en `processingIds` o `forcedDownloadIds`.
  - Fortalecer la destrucción de procesos nativos en `pauseDownloadInternal` y `requeueDownload`.

### Grupo 2: Red y Extracción de Metadatos (Prioridad Alta)
- **Problema Detectado**: La extracción de oEmbed y metadatos HTML en `ExtractionService` ejecuta llamadas bloqueantes con `withTimeoutOrNull(4000)`. Si un host tarda o se cuelga, el socket subyacente de OkHttp podía quedar activo consumiendo descriptores de archivo.
- **Acción Correctiva**:
  - Configurar timeouts de conexión y lectura específicos en el cliente de extracción (2.5s connect, 4s read).
  - Cancelar explícitamente la llamada `Call` de OkHttp en bloques `finally` ante cualquier timeout o cancelación de corrutina.
  - Asegurar que la detección de redes móviles en `isCellularNetwork` maneje correctamente VPNs y conexiones puenteadas.

### Grupo 3: Almacenamiento y Persistencia (Prioridad Media-Alta)
- **Problema Detectado**: `hasEnoughStorageSpace` y `checkStorageSpace` en `DownloadExecutor` evalúan `destFolder.absolutePath` usando `StatFs`. Si la carpeta de destino no ha sido creada en disco en ese instante, `StatFs` puede lanzar `IllegalArgumentException` no capturada en algunas versiones de Android.
- **Acción Correctiva**:
  - Asegurar la existencia de directorios (`destFolder.mkdirs()`) antes de instanciar `StatFs`, con fallback seguro al directorio de archivos interno de la app.
  - Sincronizar las operaciones de borrado físico en disco con las transacciones de Room para evitar referencias a archivos inexistentes en la galería de descargas.

### Grupo 4: UI, Ciclo de Vida y Notificaciones (Prioridad Media)
- **Problema Detectado**: En `DownloadsViewModel`, el combinador de flujos `combine(storageService.getAllDownloads(), liveProgressFlow)` emite actualizaciones con alta frecuencia. Si hay 5 o más descargas simultáneas actualizándose a 10Hz, la UI en Compose recompone elementos innecesariamente.
- **Acción Correctiva**:
  - Implementar muestreo (*throttling/conflation*) en el flujo de progreso en vivo hacia la UI (máximo 4 actualizaciones por segundo por elemento en pantalla).
  - Asegurar que `ShareReceiverActivity` valide exhaustivamente el intent de entrada (`Intent.ACTION_SEND`) contra cadenas vacías, esquemas no soportados o datos binarios inválidos antes de abrir el diálogo de descarga.

---

## 4. Arquitectura de Auditoría y Verificación

```
 ┌─────────────────────────────────────────────────────────────┐
 │                Auditoría Modular por Grupos                 │
 └──────────────────────────────┬──────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
 ┌───────────────┐       ┌───────────────┐       ┌───────────────┐
 │   Grupo 1     │       │   Grupo 2     │       │   Grupo 3/4   │
 │   Pipeline    │       │   Red & Web   │       │ Persistencia  │
 │  & Ejecución  │       │  Extracción   │       │  UI & Ciclo   │
 └──────┬────────┘       └───────┬───────┘       └───────┬───────┘
        │                        │                       │
        └───────────────────────┼───────────────────────┘
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │         Ejecución de Correcciones Atómicas por Grupo         │
 │   • Sin regresiones en APIs ni firmas existentes            │
 │   • Manejo tipado de excepciones (StorageException, etc.)   │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │               Verificación Final del Sistema                │
 │   • Compilación limpia con compile_applet                   │
 │   • Suite de pruebas unitarias JVM y Robolectric             │
 └─────────────────────────────────────────────────────────────┘
```

---

## 5. Fases de Ejecución del Plan

1. **Fase 1 (Grupo 1 - Core & Pipeline)**:
   - Limpieza y blindaje de `DownloadManagerService` y `DownloadActionReceiver`.
   - Garantía de liberación de recursos en cancelaciones y reintentos.
2. **Fase 2 (Grupo 2 - Red & Extracción)**:
   - Blindaje de timeouts y cancelación de sockets en `ExtractionService`.
   - Verificación adaptativa de redes en `isCellularNetwork`.
3. **Fase 3 (Grupo 3 - Almacenamiento & Room)**:
   - Protección de `StatFs` en `DownloadExecutor`.
   - Normalización de rutas y limpieza transaccional en `StorageService` y `PathUtils`.
4. **Fase 4 (Grupo 4 - UI, Ciclo de Vida y Notificaciones)**:
   - Optimización de tasa de emisión en `DownloadsViewModel`.
   - Blindaje de intents en `ShareReceiverActivity`.
5. **Fase 5 (Validación Global)**:
   - Ejecución de `compile_applet` y suite completa de pruebas unitarias (`testDebugUnitTest`).
