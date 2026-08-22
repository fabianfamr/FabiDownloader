package com.fabian.downloader.services.sites

class SpotifyService : BaseSiteService() {
    override val siteId: String = "spotify"
    override val displayName: String = "Spotify"
    override val brandColorHex: String = "#1DB954"
    override val iconName: String = "spotify"
    override val supportedUrlPatterns: List<String> = listOf("spotify.com")
}
