package com.fabian.downloader.services.sites

class BandcampService : BaseSiteService() {
    override val siteId: String = "bandcamp"
    override val displayName: String = "Bandcamp"
    override val brandColorHex: String = "#629AA9"
    override val iconName: String = "bandcamp"
    override val supportedUrlPatterns: List<String> = listOf("bandcamp.com")
}
