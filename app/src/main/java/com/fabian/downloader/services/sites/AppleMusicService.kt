package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config

class AppleMusicService : BaseSiteService() {
    override val siteId: String = "applemusic"
    override val displayName: String = "Apple Music"
    override val brandColorHex: String = "#FA243C"
    override val iconName: String = "applemusic"
    override val supportedUrlPatterns: List<String> = listOf("music.apple.com")

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

