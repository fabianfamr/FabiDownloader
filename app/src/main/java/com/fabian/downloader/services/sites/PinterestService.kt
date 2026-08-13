package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class PinterestService : BaseSiteService() {
    override val siteId: String = "pinterest"
    override val displayName: String = "Pinterest"
    override val brandColorHex: String = "#E60023"
    override val iconName: String = "pinterest"
    override val supportedUrlPatterns: List<String> = listOf("pinterest.com", "pin.it")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
