package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class PinterestService : BaseSiteService() {
    override val siteId: String = "pinterest"
    override val displayName: String = "Pinterest"
    override val brandColorHex: String = "#E60023"
    override val iconName: String = "pinterest"
    override val supportedUrlPatterns: List<String> = listOf("pinterest.com", "pin.it")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://www.pinterest.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

