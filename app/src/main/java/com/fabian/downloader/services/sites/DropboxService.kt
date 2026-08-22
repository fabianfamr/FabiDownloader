package com.fabian.downloader.services.sites

class DropboxService : BaseSiteService() {
    override val siteId: String = "dropbox"
    override val displayName: String = "Dropbox"
    override val brandColorHex: String = "#0061FF"
    override val iconName: String = "dropbox"
    override val supportedUrlPatterns: List<String> = listOf("dropbox.com", "db.tt")
}
