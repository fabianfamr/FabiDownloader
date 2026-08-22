package com.fabian.downloader.services.sites

class TumblrService : BaseSiteService() {
    override val siteId: String = "tumblr"
    override val displayName: String = "Tumblr"
    override val brandColorHex: String = "#36465D"
    override val iconName: String = "tumblr"
    override val supportedUrlPatterns: List<String> = listOf("tumblr.com")
}
