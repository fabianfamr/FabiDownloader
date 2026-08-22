package com.fabian.downloader.services.sites

class DailymotionService : BaseSiteService() {
    override val siteId: String = "dailymotion"
    override val displayName: String = "Dailymotion"
    override val brandColorHex: String = "#0066DC"
    override val iconName: String = "dailymotion"
    override val supportedUrlPatterns: List<String> = listOf("dailymotion.com", "dai.ly")
}
