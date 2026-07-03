package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class TwitchService : BaseSiteService() {
    override val siteId: String = "twitch"
    override val displayName: String = "Twitch"
    override val brandColorHex: String = "#9146FF"
    override val iconName: String = "twitch"
    override val supportedUrlPatterns: List<String> = listOf("twitch.tv")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
    }
}
