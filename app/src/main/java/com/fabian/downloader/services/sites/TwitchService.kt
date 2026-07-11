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
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
