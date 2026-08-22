package com.fabian.downloader.services.sites

class TelegramService : BaseSiteService() {
    override val siteId: String = "telegram"
    override val displayName: String = "Telegram"
    override val brandColorHex: String = "#26A5E4"
    override val iconName: String = "telegram"
    override val supportedUrlPatterns: List<String> = listOf("t.me", "telegram.org")
}
