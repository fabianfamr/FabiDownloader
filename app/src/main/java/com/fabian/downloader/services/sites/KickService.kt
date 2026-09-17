package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class KickService : BaseSiteService() {
    override val siteId: String = "kick"
    override val displayName: String = "Kick"
    override val brandColorHex: String = "#53FC18"
    override val iconName: String = "kick"
    override val supportedUrlPatterns: List<String> = listOf("kick.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://kick.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

