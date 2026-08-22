package com.fabian.downloader.services.sites

class BilibiliService : BaseSiteService() {
    override val siteId: String = "bilibili"
    override val displayName: String = "Bilibili"
    override val brandColorHex: String = "#00A1D6"
    override val iconName: String = "bilibili"
    override val supportedUrlPatterns: List<String> = listOf("bilibili.com", "b23.tv")
}
