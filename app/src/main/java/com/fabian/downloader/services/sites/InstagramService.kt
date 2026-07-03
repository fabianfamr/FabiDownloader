package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class InstagramService : BaseSiteService() {
    override val siteId: String = "instagram"
    override val displayName: String = "Instagram"
    override val brandColorHex: String = "#E1306C"
    override val iconName: String = "instagram"
    override val supportedUrlPatterns: List<String> = listOf("instagram.com")
}
