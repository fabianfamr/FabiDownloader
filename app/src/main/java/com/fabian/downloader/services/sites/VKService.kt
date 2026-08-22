package com.fabian.downloader.services.sites

class VKService : BaseSiteService() {
    override val siteId: String = "vk"
    override val displayName: String = "VK"
    override val brandColorHex: String = "#0077FF"
    override val iconName: String = "vk"
    override val supportedUrlPatterns: List<String> = listOf("vk.com", "vk.ru")
}
