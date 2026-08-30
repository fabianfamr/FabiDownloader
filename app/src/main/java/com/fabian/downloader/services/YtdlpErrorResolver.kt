package com.fabian.downloader.services

import com.fabian.downloader.R
import com.fabian.downloader.MyApplication
import com.fabian.downloader.ui.AppSettings
import com.fabian.downloader.utils.LocaleHelper

object YtdlpErrorResolver {

    fun resolveUserFacingError(e: Throwable, lastLine: String): String {
        val appCtx = try { MyApplication.getInstance() } catch (_: Exception) { null }
        val ctx = if (appCtx != null) {
            LocaleHelper.applyLocale(appCtx, AppSettings.language)
        } else null

        fun getStr(resId: Int, fallback: String): String = ctx?.getString(resId) ?: fallback

        val actualException = if (e is Exception && e.cause != null && e.message?.contains(e.cause!!.javaClass.name) == true) e.cause!! else e
        val msg = actualException.message ?: e.message ?: ""
        val lowerMsg = msg.lowercase()
        val lowerClass = actualException.javaClass.name.lowercase()
        val lowerLine = lastLine.lowercase()

        if (lowerClass.contains("youtubedlexception") || lowerClass.contains("youtubedl") || lowerMsg.contains("youtubedl")) {
            if (lowerMsg.contains("process id already exists")) {
                return getStr(R.string.downloads_error_process_stuck, "Proceso atascado")
            }
            if (lowerMsg.contains("sign in") || lowerMsg.contains("private video") || lowerMsg.contains("login")) {
                return getStr(R.string.downloads_error_requires_login, "Requiere inicio de sesión")
            }
            if (lowerMsg.contains("unavailable") || lowerLine.contains("unavailable")) {
                return getStr(R.string.downloads_error_unavailable, "No disponible")
            }
            if (lowerMsg.contains("canceled") || lowerClass.contains("canceled")) {
                return getStr(R.string.downloads_error_interrupted, "Descarga interrumpida")
            }
            if (lowerMsg.contains("http error 403")) {
                return getStr(R.string.downloads_error_access_denied, "Acceso denegado (403)")
            }
            return "YT-DLP: ${msg.take(30)}..."
        }
        
        if (actualException is java.io.InterruptedIOException || lowerClass.contains("interruptedioexception") || lowerMsg.contains("interrupted")) {
            return getStr(R.string.downloads_error_interrupted, "Descarga interrumpida")
        }
        
        if (lowerMsg.contains("no space left") || lowerMsg.contains("enospc") || lowerMsg.contains("disk full")) {
            return getStr(R.string.downloads_error_storage, "Espacio insuficiente")
        }
        if (lowerMsg.contains("timeout") || lowerMsg.contains("connection") || lowerMsg.contains("network")) {
            return getStr(R.string.downloads_error_network_retry, "Error de red")
        }
        
        val fallBackMsg = lastLine.ifEmpty { msg }
        return fallBackMsg.ifEmpty { getStr(R.string.downloads_error_unknown, "Error desconocido") }
    }

    fun isNetworkOrTemporaryError(e: Throwable, line: String): Boolean {
        if (e is java.io.InterruptedIOException) return true
        val lowerMsg = (e.message ?: "").lowercase()
        val lowerClass = e.javaClass.name.lowercase()
        val lowerLine = line.lowercase()
        val keywords = listOf(
            "timeout", "time out", "timed out", "connection", "unable to resolve host", 
            "network is unreachable", "502", "503", "504", "429", "403", "http error 403", "http error 429",
            "read error", "connection reset", "connection refused", "broken pipe", "ssl", "socket", "try again",
            "quickjs", "solving js challenges", "streamgobbler",
            "read interrupted", "interruptedioexception", "signature extraction",
            "unable to extract", "temporary failure", "handshake", "end of file", "eof",
            "connection closed", "unexpected end of stream", "software caused connection abort"
        )
        return keywords.any { lowerMsg.contains(it) || lowerClass.contains(it) || lowerLine.contains(it) }
    }

    fun isFatalUnrecoverableError(e: Throwable, line: String): Boolean {
        val lowerMsg = (e.message ?: "").lowercase()
        val lowerClass = e.javaClass.name.lowercase()
        val lowerLine = line.lowercase()
        val fatalKeywords = listOf(
            "private video", "this video is private", "video unavailable",
            "this video has been removed", "account has been terminated",
            "http error 404", "404 not found", "requires payment",
            "members-only content", "drm protected", "login required",
            "sign in to confirm your age", "this video is only available to registered users"
        )
        return fatalKeywords.any { lowerMsg.contains(it) || lowerClass.contains(it) || lowerLine.contains(it) }
    }
}
