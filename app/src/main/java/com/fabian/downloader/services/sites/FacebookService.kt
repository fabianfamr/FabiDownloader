package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class FacebookService : BaseSiteService() {
    override val siteId: String = "facebook"
    override val displayName: String = "Facebook"
    override val brandColorHex: String = "#1877F2"
    override val iconName: String = "facebook"
    override val supportedUrlPatterns: List<String> = listOf("facebook.com", "fb.watch", "fb.com", "m.facebook.com", "web.facebook.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://www.facebook.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--add-header", "Accept-Language: es-ES,es;q=0.9,en;q=0.8")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--add-header", "Accept-Language: es-ES,es;q=0.9,en;q=0.8")
    }
}

