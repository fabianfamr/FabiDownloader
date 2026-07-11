package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class TikTokService : BaseSiteService() {
    override val siteId: String = "tiktok"
    override val displayName: String = "TikTok"
    override val brandColorHex: String = "#00F2FE"
    override val iconName: String = "tiktok"
    override val supportedUrlPatterns: List<String> = listOf("tiktok.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_TIKTOK_MOBILE)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_TIKTOK_MOBILE)
    }
}
