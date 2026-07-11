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
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
