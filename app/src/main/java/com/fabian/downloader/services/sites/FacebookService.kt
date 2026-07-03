package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class FacebookService : BaseSiteService() {
    override val siteId: String = "facebook"
    override val displayName: String = "Facebook"
    override val brandColorHex: String = "#1877F2"
    override val iconName: String = "facebook"
    override val supportedUrlPatterns: List<String> = listOf("facebook.com", "fb.watch", "fb.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    }
}
