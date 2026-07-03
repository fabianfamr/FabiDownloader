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
        request.addOption("--user-agent", "Mozilla/5.0 (Linux; Android 10; SM-G960F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Mobile Safari/537.36")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Linux; Android 10; SM-G960F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Mobile Safari/537.36")
    }
}
