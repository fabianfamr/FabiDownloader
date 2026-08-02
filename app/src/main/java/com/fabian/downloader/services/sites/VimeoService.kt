package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class VimeoService : BaseSiteService() {
    override val siteId: String = "vimeo"
    override val displayName: String = "Vimeo"
    override val brandColorHex: String = "#1AB7EA"
    override val iconName: String = "vimeo"
    override val supportedUrlPatterns: List<String> = listOf("vimeo.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
