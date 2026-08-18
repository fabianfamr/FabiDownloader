package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class InstagramService : BaseSiteService() {
    override val siteId: String = "instagram"
    override val displayName: String = "Instagram"
    override val brandColorHex: String = "#E1306C"
    override val iconName: String = "instagram"
    override val supportedUrlPatterns: List<String> = listOf("instagram.com")

    // Instagram bloquea frecuentemente User-Agents genéricos: usar un UA móvil de iPhone
    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", Config.UA_INSTAGRAM)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", Config.UA_INSTAGRAM)
    }
}
