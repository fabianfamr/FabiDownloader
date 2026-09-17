package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config

class BandcampService : BaseSiteService() {
    override val siteId: String = "bandcamp"
    override val displayName: String = "Bandcamp"
    override val brandColorHex: String = "#629AA9"
    override val iconName: String = "bandcamp"
    override val supportedUrlPatterns: List<String> = listOf("bandcamp.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.PLAYLISTS,
        SiteCapability.EMBED_METADATA
    )

    override val supportedFormats: List<String> = listOf(Config.FORMAT_MP3, Config.FORMAT_M4A)
    override val supportedQualities: List<String> = listOf("320kbps", "256kbps", "192kbps", "128kbps")
    override val defaultQuality: String = "320kbps"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }
}

