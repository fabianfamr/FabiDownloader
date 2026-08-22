package com.fabian.downloader.services.sites

class RumbleService : BaseSiteService() {
    override val siteId: String = "rumble"
    override val displayName: String = "Rumble"
    override val brandColorHex: String = "#85C742"
    override val iconName: String = "rumble"
    override val supportedUrlPatterns: List<String> = listOf("rumble.com")
}
