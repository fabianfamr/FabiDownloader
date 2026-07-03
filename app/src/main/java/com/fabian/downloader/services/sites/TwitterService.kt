package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class TwitterService : BaseSiteService() {
    override val siteId: String = "twitter"
    override val displayName: String = "Twitter / X"
    override val brandColorHex: String = "#1DA1F2"
    override val iconName: String = "twitter"
    override val supportedUrlPatterns: List<String> = listOf("twitter.com", "x.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
    }
}
