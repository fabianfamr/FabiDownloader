package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class VimeoService : BaseSiteService() {
    override val siteId: String = "vimeo"
    override val displayName: String = "Vimeo"
    override val brandColorHex: String = "#1AB7EA"
    override val iconName: String = "vimeo"
    override val supportedUrlPatterns: List<String> = listOf("vimeo.com", "player.vimeo.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.SUBTITLES,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://vimeo.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

