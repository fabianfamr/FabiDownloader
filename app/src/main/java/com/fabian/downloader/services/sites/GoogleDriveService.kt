package com.fabian.downloader.services.sites

class GoogleDriveService : BaseSiteService() {
    override val siteId: String = "googledrive"
    override val displayName: String = "Google Drive"
    override val brandColorHex: String = "#4285F4"
    override val iconName: String = "googledrive"
    override val supportedUrlPatterns: List<String> = listOf("drive.google.com")
}
