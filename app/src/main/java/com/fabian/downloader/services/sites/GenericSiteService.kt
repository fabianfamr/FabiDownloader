package com.fabian.downloader.services.sites

class GenericSiteService : BaseSiteService() {
    override val siteId: String = "generic"
    override val displayName: String = "Enlace Directo"
    override val brandColorHex: String = "#607D8B"
    override val iconName: String = "generic"
    override val supportedUrlPatterns: List<String> = emptyList()
}
