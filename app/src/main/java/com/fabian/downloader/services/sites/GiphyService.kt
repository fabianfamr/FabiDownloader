package com.fabian.downloader.services.sites

class GiphyService : BaseSiteService() {
    override val siteId: String = "giphy"
    override val displayName: String = "Giphy"
    override val brandColorHex: String = "#000000"
    override val iconName: String = "giphy"
    override val supportedUrlPatterns: List<String> = listOf("giphy.com")
}
