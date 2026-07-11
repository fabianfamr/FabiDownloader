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
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
