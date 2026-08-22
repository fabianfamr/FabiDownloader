package com.fabian.downloader.services.sites

class PatreonService : BaseSiteService() {
    override val siteId: String = "patreon"
    override val displayName: String = "Patreon"
    override val brandColorHex: String = "#FF424D"
    override val iconName: String = "patreon"
    override val supportedUrlPatterns: List<String> = listOf("patreon.com")
}
