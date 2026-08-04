package com.fabian.downloader.services.sites

class YouTubeService : BaseSiteService() {
    override val siteId: String = "youtube"
    override val displayName: String = "YouTube"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube"
    override val supportedUrlPatterns: List<String> = listOf("youtube.com", "youtu.be", "music.youtube.com")
}
