package com.fabian.downloader.download

import java.io.File

/**
 * MODELO DE TAREA DE DESCARGA (estructura portada de Seal: `download/Task.kt`)
 *
 * Representa una unidad de trabajo para el motor de descargas. La UI de
 * FabiDownloader NO consume este modelo directamente (sigue leyendo los
 * `DownloadRecord` de Room), por lo que aquí no se persiste nada: la tarea es
 * un contrato interno entre la cola existente y el motor ejecutor.
 */
data class DownloadTask(
    val url: String,
    val quality: String,
    val format: String,
    val destFolder: File,
    val fileNameWithoutExt: String,
    val taskId: String,
    val title: String = "",
    val type: DownloadTask.TypeInfo = DownloadTask.TypeInfo.URL,
) : Comparable<DownloadTask> {

    val timeCreated: Long = System.currentTimeMillis()

    override fun compareTo(other: DownloadTask): Int = timeCreated.compareTo(other.timeCreated)

    /** Identidad de la tarea, usada como processId de yt-dlp. */
    val notificationId: Int
        get() = taskId.hashCode()

    sealed interface TypeInfo {
        /** Elemento individual (o enlace directo). */
        data object URL : TypeInfo

        /** Elemento de una lista de reproducción, identificado por su índice. */
        data class Playlist(val index: Int = 0) : TypeInfo
    }

    /** Estados por los que pasa una tarea (espejo del ciclo de vida de Seal). */
    sealed interface DownloadState {
        val isTerminal: Boolean
            get() = this is Completed || this is Error || this is Canceled

        data object Idle : DownloadState
        data object ReadyWithInfo : DownloadState
        data class Running(val taskId: String, val progress: Float = -1f, val progressText: String = "") : DownloadState
        data class Canceled(val action: RestartableAction) : DownloadState
        data class Error(val throwable: Throwable, val action: RestartableAction) : DownloadState
        data class Completed(val filePath: String? = null) : DownloadState
    }

    sealed interface RestartableAction {
        data object Download : RestartableAction
        data object FetchInfo : RestartableAction
    }

    companion object {
        /** Progreso indeterminado (igual que en Seal). */
        const val PROGRESS_INDETERMINATE = -1f
    }
}
