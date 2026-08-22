package com.fabian.downloader.services.sites

class ImgurService : BaseSiteService() {
    override val siteId: String = "imgur"
    override val displayName: String = "Imgur"
    override val brandColorHex: String = "#1BB76E"
    override val iconName: String = "imgur"
    override val supportedUrlPatterns: List<String> = listOf("imgur.com")
}
