package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config

class DailymotionService : BaseSiteService() {
    override val siteId: String = "dailymotion"
    override val displayName: String = "Dailymotion"
    override val brandColorHex: String = "#0066DC"
    override val iconName: String = "dailymotion"
    override val supportedUrlPatterns: List<String> = listOf("dailymotion.com", "dai.ly")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://www.dailymotion.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

