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
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
