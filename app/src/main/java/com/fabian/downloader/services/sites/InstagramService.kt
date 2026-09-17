package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class InstagramService : BaseSiteService() {
    override val siteId: String = "instagram"
    override val displayName: String = "Instagram"
    override val brandColorHex: String = "#E1306C"
    override val iconName: String = "instagram"
    override val supportedUrlPatterns: List<String> = listOf("instagram.com", "instagr.am")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_INSTAGRAM
    override val customReferer: String = "https://www.instagram.com/"

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

