package com.fabian.downloader.utils

import org.json.JSONObject
import org.json.JSONArray
import com.fabian.downloader.services.InfoMedia

object YtdlpParser {

    fun cleanTitleOfSuffixes(title: String): String {
        var clean = title.trim()
        
        // Remove " - Topic" (common in YouTube Music auto-generated uploads)
        if (clean.endsWith(" - Topic", ignoreCase = true)) {
            clean = clean.substring(0, clean.length - 8).trim()
        } else if (clean.endsWith("- Topic", ignoreCase = true)) {
            clean = clean.substring(0, clean.length - 7).trim()
        }
        
        // Remove common video/audio suffixes like " - video", " - audio", " - video - audio", " - audio - video"
        val suffixesToRemove = listOf(
            " - video - audio",
            " - audio - video",
            " - video",
            " - audio",
            " - mp4",
            " - mp3",
            " (video)",
            " (audio)",
            " [video]",
            " [audio]",
            "-video-audio",
            "-video",
            "-audio"
        )
        
        for (suffix in suffixesToRemove) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length).trim()
            }
        }
        
        return clean
    }

    private fun cleanDescriptionForTitle(description: String): String {
        if (description.isEmpty()) return ""
        
        // Tomar la primera línea que no esté vacía
        val lines = description.split("\n")
        var firstLine = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                firstLine = trimmed
                break
            }
        }
        
        if (firstLine.isEmpty()) return ""
        
        // Limitar longitud
        if (firstLine.length > 80) {
            firstLine = firstLine.substring(0, 77) + "..."
        }
        return firstLine
    }

    fun parseMetadata(json: JSONObject, defaultAuthor: String = "Desconocido", defaultTitle: String = "Video sin título"): InfoMedia {
        var rawTitle = json.optString("title", "").trim()
        val description = json.optString("description", "").trim()
        val uploader = json.optString("uploader", json.optString("uploader_id", "")).trim()
        val webpageUrl = json.optString("webpage_url", "").lowercase()
        val extractor = json.optString("extractor", "").lowercase()
        val extractorKey = json.optString("extractor_key", "").lowercase()

        val isInstagram = webpageUrl.contains("instagram") || extractor.contains("instagram") || extractorKey.contains("instagram") || defaultTitle.lowercase().contains("instagram")
        val isTikTok = webpageUrl.contains("tiktok") || extractor.contains("tiktok") || extractorKey.contains("tiktok") || defaultTitle.lowercase().contains("tiktok")
        val isTwitch = webpageUrl.contains("twitch") || extractor.contains("twitch") || extractorKey.contains("twitch") || defaultTitle.lowercase().contains("twitch")
        val isKick = webpageUrl.contains("kick") || extractor.contains("kick") || extractorKey.contains("kick") || defaultTitle.lowercase().contains("kick")
        val isTwitter = webpageUrl.contains("twitter") || webpageUrl.contains("x.com") || extractor.contains("twitter") || extractorKey.contains("twitter") || defaultTitle.lowercase().contains("twitter") || defaultTitle.lowercase().contains("x.com")

        val genericTitles = listOf(
            "instagram video", "instagram photo", "instagram post", "instagram reel", "video de instagram", "video sin título", "desconocido", "video", "post", "audio", "directo", "live", "reels", "reel", "tiktok video", "video de tiktok"
        )
        
        var isTitleGeneric = rawTitle.isEmpty() || genericTitles.any { rawTitle.lowercase() == it } || rawTitle.lowercase().contains("video de ")

        if (isTitleGeneric) {
            if (isInstagram) {
                val cleanDesc = cleanDescriptionForTitle(description)
                rawTitle = if (cleanDesc.isNotEmpty()) {
                    if (uploader.isNotEmpty()) "@$uploader: $cleanDesc" else cleanDesc
                } else if (uploader.isNotEmpty()) {
                    "Publicación de @$uploader"
                } else {
                    defaultTitle
                }
            } else if (isTikTok) {
                val cleanDesc = cleanDescriptionForTitle(description)
                rawTitle = if (cleanDesc.isNotEmpty()) {
                    if (uploader.isNotEmpty()) "@$uploader: $cleanDesc" else cleanDesc
                } else if (uploader.isNotEmpty()) {
                    "Video de @$uploader"
                } else {
                    defaultTitle
                }
            } else if (isTwitter) {
                val cleanDesc = cleanDescriptionForTitle(description)
                rawTitle = if (cleanDesc.isNotEmpty()) {
                    if (uploader.isNotEmpty()) "@$uploader: $cleanDesc" else cleanDesc
                } else if (uploader.isNotEmpty()) {
                    "Tweet de @$uploader"
                } else {
                    defaultTitle
                }
            } else if (isTwitch || isKick) {
                val streamTitle = json.optString("title", "").trim()
                val channelName = uploader.ifEmpty { json.optString("channel", "").trim() }
                rawTitle = if (streamTitle.isNotEmpty() && !streamTitle.equals("twitch", ignoreCase = true) && !streamTitle.equals("kick", ignoreCase = true)) {
                    if (channelName.isNotEmpty() && !streamTitle.contains(channelName)) {
                        "$channelName - $streamTitle"
                    } else {
                        streamTitle
                    }
                } else if (channelName.isNotEmpty()) {
                    "Directo de $channelName"
                } else {
                    defaultTitle
                }
            } else {
                val cleanDesc = cleanDescriptionForTitle(description)
                rawTitle = if (cleanDesc.isNotEmpty()) {
                    cleanDesc
                } else {
                    rawTitle.ifEmpty { defaultTitle }
                }
            }
        }

        val title = cleanTitleOfSuffixes(rawTitle.ifEmpty { defaultTitle })

        var author = json.optString("uploader", json.optString("uploader_id", defaultAuthor)).trim()
        if (author.endsWith(" - Topic", ignoreCase = true)) {
            author = author.substring(0, author.length - 8).trim()
        } else if (author.endsWith("- Topic", ignoreCase = true)) {
            author = author.substring(0, author.length - 7).trim()
        }
        val miniatura = json.optString("thumbnail", "")
        val duracion = json.optString("duration_string", "00:00")
        val duracionSegundos = json.optDouble("duration", 0.0)
        val vistas = json.optString("view_count", "0")
        val videoId = json.optString("id", "")

        val sizesMap = mutableMapOf<String, Double>()
        var tamañoBytesMax = 0L
        val formats = json.optJSONArray("formats")
        
        if (formats != null) {
            val bestAudioSize = getBestAudioSize(formats, duracionSegundos)
            
            for (i in 0 until formats.length()) {
                val formatoObj = formats.optJSONObject(i) ?: continue
                val height = formatoObj.optInt("height", 0)
                val width = formatoObj.optInt("width", 0)
                val realHeight = if (height > 0) height else if (width > 0) {
                    // Estimar altura para videos verticales o formatos extraños si falta height
                    if (width > height) (width * 9 / 16) else width
                } else 0
                
                val ext = formatoObj.optString("ext", "")
                val vcodec = formatoObj.optString("vcodec", "")
                var bytes = getFilesize(formatoObj, duracionSegundos)
                
                // Si bytes sigue siendo 0, estimar usando bitrates estándares basados en resolución y duración
                if (bytes <= 0L && duracionSegundos > 0.0) {
                    val estimatedBitrateKbps = when {
                        realHeight >= 2160 -> 12000.0 // 4K
                        realHeight >= 1440 -> 6000.0  // 2K
                        realHeight >= 1080 -> 3500.0  // 1080p
                        realHeight >= 720 -> 1800.0   // 720p
                        realHeight >= 480 -> 800.0    // 480p
                        realHeight >= 360 -> 400.0    // 360p
                        realHeight >= 240 -> 250.0    // 240p
                        vcodec == "none" || vcodec.contains("audio only") -> {
                            if (ext == "m4a") 128.0 else 192.0
                        }
                        else -> 800.0
                    }
                    bytes = ((estimatedBitrateKbps * 1000.0 / 8.0) * duracionSegundos).toLong()
                }
                
                val totalBytes = if (bytes > 0 && vcodec != "none" && !vcodec.contains("audio only")) bytes + bestAudioSize else bytes
                val mb = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else 0.0
                
                if (vcodec == "none" || vcodec.contains("audio only")) {
                    if (ext == "m4a") sizesMap["audio_m4a"] = mb
                    if (ext == "mp3" || vcodec == "none" || vcodec.contains("audio only")) sizesMap["audio_mp3"] = mb
                } else if (realHeight > 0) {
                    sizesMap["video_${realHeight}p"] = mb
                }
                
                if (totalBytes > tamañoBytesMax) tamañoBytesMax = totalBytes
            }
        }
        val pesoMB = if (tamañoBytesMax > 0L) tamañoBytesMax / (1024.0 * 1024.0) else 0.0

        return InfoMedia(title, author, miniatura, duracion, vistas, pesoMB, videoId, sizesMap)
    }

    fun getFilesize(formatoObj: JSONObject, durationSeconds: Double = 0.0): Long {
        val filesize = if (formatoObj.has("filesize") && !formatoObj.isNull("filesize")) {
            formatoObj.optLong("filesize", 0L)
        } else {
            0L
        }
        if (filesize > 0L) return filesize

        val filesizeApprox = if (formatoObj.has("filesize_approx") && !formatoObj.isNull("filesize_approx")) {
            formatoObj.optLong("filesize_approx", 0L)
        } else {
            0L
        }
        if (filesizeApprox > 0L) return filesizeApprox

        // Si no está disponible, estimar usando bitrate (tbr, vbr, abr) y la duración en segundos
        if (durationSeconds > 0.0) {
            val tbr = formatoObj.optDouble("tbr", 0.0)
            val vbr = formatoObj.optDouble("vbr", 0.0)
            val abr = formatoObj.optDouble("abr", 0.0)
            val totalBitrate = if (tbr > 0.0) tbr else (vbr + abr)
            if (totalBitrate > 0.0) {
                // totalBitrate está en kbps. (kbps * 1000 / 8) * segundos = bytes
                val estimatedBytes = (totalBitrate * 1000.0 / 8.0) * durationSeconds
                if (estimatedBytes > 0.0) {
                    return estimatedBytes.toLong()
                }
            }
        }

        return 0L
    }

    fun getBestAudioSize(formats: JSONArray, durationSeconds: Double = 0.0): Long {
        var bestAudioSize = 0L
        for (i in 0 until formats.length()) {
            val formatoObj = formats.optJSONObject(i) ?: continue
            val vcodec = formatoObj.optString("vcodec", "")
            if (vcodec == "none") {
                val bytes = getFilesize(formatoObj, durationSeconds)
                if (bytes > bestAudioSize) {
                    bestAudioSize = bytes
                }
            }
        }
        return bestAudioSize
    }
}
