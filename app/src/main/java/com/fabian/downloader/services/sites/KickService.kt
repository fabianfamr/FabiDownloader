package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class KickService : BaseSiteService() {
    override val siteId: String = "kick"
    override val displayName: String = "Kick"
    override val brandColorHex: String = "#53FC18"
    override val iconName: String = "kick"
    override val supportedUrlPatterns: List<String> = listOf("kick.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
    }
}
