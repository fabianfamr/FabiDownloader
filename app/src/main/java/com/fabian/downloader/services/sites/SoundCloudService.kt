package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class SoundCloudService : BaseSiteService() {
    override val siteId: String = "soundcloud"
    override val displayName: String = "SoundCloud"
    override val brandColorHex: String = "#FF5500"
    override val iconName: String = "soundcloud"
    override val supportedUrlPatterns: List<String> = listOf("soundcloud.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
    }
}
