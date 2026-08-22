package com.fabian.downloader.services.sites

class AppleMusicService : BaseSiteService() {
    override val siteId: String = "applemusic"
    override val displayName: String = "Apple Music"
    override val brandColorHex: String = "#FA243C"
    override val iconName: String = "applemusic"
    override val supportedUrlPatterns: List<String> = listOf("music.apple.com")
}
