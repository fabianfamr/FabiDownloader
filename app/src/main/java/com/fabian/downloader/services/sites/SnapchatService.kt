package com.fabian.downloader.services.sites

class SnapchatService : BaseSiteService() {
    override val siteId: String = "snapchat"
    override val displayName: String = "Snapchat"
    override val brandColorHex: String = "#FFFC00"
    override val iconName: String = "snapchat"
    override val supportedUrlPatterns: List<String> = listOf("snapchat.com")
}
