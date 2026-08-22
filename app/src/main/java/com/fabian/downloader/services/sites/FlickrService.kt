package com.fabian.downloader.services.sites

class FlickrService : BaseSiteService() {
    override val siteId: String = "flickr"
    override val displayName: String = "Flickr"
    override val brandColorHex: String = "#0063DC"
    override val iconName: String = "flickr"
    override val supportedUrlPatterns: List<String> = listOf("flickr.com")
}
