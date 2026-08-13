package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class RedditService : BaseSiteService() {
    override val siteId: String = "reddit"
    override val displayName: String = "Reddit"
    override val brandColorHex: String = "#FF4500"
    override val iconName: String = "reddit"
    override val supportedUrlPatterns: List<String> = listOf("reddit.com", "v.redd.it")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
