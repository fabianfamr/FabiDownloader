package com.fabian.downloader.utils

import com.fabian.downloader.MyApplication
import com.fabian.downloader.R
import com.fabian.downloader.ui.AppSettings

/**
 * Acceso a recursos de texto (strings.xml) desde clases sin Context
 * (objetos singleton, extractores nativos, servicios ligeros).
 *
 * Usa el contexto de la Application aplicando el idioma elegido en la app
 * (mismo patrón que YtdlpErrorResolver), con fallback seguro al literal
 * original si la app aún no está inicializada.
 */
object AppStrings {

    private fun localizedString(resId: Int, fallback: String): String {
        return try {
            val appCtx = MyApplication.getInstance()
            LocaleHelper.applyLocale(appCtx, AppSettings.language).getString(resId)
        } catch (_: Exception) {
            fallback
        }
    }

    fun get(resId: Int, fallback: String): String = localizedString(resId, fallback)

    fun get(resId: Int, formatArg: String, fallback: String): String = try {
        val appCtx = MyApplication.getInstance()
        LocaleHelper.applyLocale(appCtx, AppSettings.language).getString(resId, formatArg)
    } catch (_: Exception) {
        fallback.replace("%1\$s", formatArg)
    }

    // --- Títulos de descarga (se guardan en el historial) --------------------

    fun videoOf(author: String): String =
        get(R.string.share_video_of, author, "Video de %1\$s".replace("%1\$s", author))

    fun publicationOf(author: String): String =
        get(R.string.title_publication_of, author, "Publicación de %1\$s".replace("%1\$s", author))

    fun tweetOf(author: String): String =
        get(R.string.title_tweet_of, author, "Tweet de %1\$s".replace("%1\$s", author))

    fun liveOf(channel: String): String =
        get(R.string.title_live_of, channel, "Directo de %1\$s".replace("%1\$s", channel))

    fun directLink(): String = get(R.string.share_direct_link, "Enlace Directo")

    fun playlistDefault(): String = get(R.string.playlist_default_title, "Lista de reproducción")

    fun unknownAuthor(): String = get(R.string.title_unknown_author, "Desconocido")

    fun untitledVideo(): String = get(R.string.status_default_title, "Video sin título")

    fun instagramFallback(): String = get(R.string.title_fallback_instagram, "Instagram Video")

    fun tiktokFallback(): String = get(R.string.title_fallback_tiktok, "Video de TikTok")

    fun redditFallback(): String = get(R.string.title_fallback_reddit, "Publicación de Reddit")

    fun twitterFallback(): String = get(R.string.title_fallback_twitter, "Video de X / Twitter")

    fun youtubeFallback(): String = get(R.string.title_fallback_youtube, "Video de YouTube")

    fun noWatermarkHd(): String = get(R.string.quality_no_watermark_hd, "HD (Sin Marca)")

    fun cancelledByUser(): String = get(R.string.status_cancelled_by_user, "Cancelado por usuario")

    fun emptySegment(segment: Int): String = try {
        val appCtx = MyApplication.getInstance()
        LocaleHelper.applyLocale(appCtx, AppSettings.language)
            .getString(R.string.error_empty_segment, segment)
    } catch (_: Exception) {
        "Cuerpo vacío en segmento $segment"
    }

    // --- Rutas amigables (PathUtils) -----------------------------------------

    fun downloadsAppFolder(): String = get(R.string.path_downloads_app, "Descargas > FabiDownloader")

    fun internalStorage(): String = get(R.string.path_internal_storage, "Almacenamiento interno")

    fun sdCard(): String = get(R.string.path_sd_card, "Tarjeta SD")
}
