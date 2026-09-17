package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config

class BilibiliService : BaseSiteService() {
    override val siteId: String = "bilibili"
    override val displayName: String = "Bilibili"
    override val brandColorHex: String = "#00A1D6"
    override val iconName: String = "bilibili"
    override val supportedUrlPatterns: List<String> = listOf("bilibili.com", "b23.tv")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://www.bilibili.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

