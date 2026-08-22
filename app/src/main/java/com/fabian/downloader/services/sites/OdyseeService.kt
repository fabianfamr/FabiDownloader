package com.fabian.downloader.services.sites

class OdyseeService : BaseSiteService() {
    override val siteId: String = "odysee"
    override val displayName: String = "Odysee"
    override val brandColorHex: String = "#E21B4D"
    override val iconName: String = "odysee"
    override val supportedUrlPatterns: List<String> = listOf("odysee.com")
}
