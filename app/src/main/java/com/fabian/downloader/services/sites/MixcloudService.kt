package com.fabian.downloader.services.sites

class MixcloudService : BaseSiteService() {
    override val siteId: String = "mixcloud"
    override val displayName: String = "Mixcloud"
    override val brandColorHex: String = "#5000FF"
    override val iconName: String = "mixcloud"
    override val supportedUrlPatterns: List<String> = listOf("mixcloud.com")
}
