package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config

class SpotifyService : BaseSiteService() {
    override val siteId: String = "spotify"
    override val displayName: String = "Spotify"
    override val brandColorHex: String = "#1DB954"
    override val iconName: String = "spotify"
    override val supportedUrlPatterns: List<String> = listOf("spotify.com", "open.spotify.com")

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

