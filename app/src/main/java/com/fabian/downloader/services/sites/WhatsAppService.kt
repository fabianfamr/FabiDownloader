package com.fabian.downloader.services.sites

class WhatsAppService : BaseSiteService() {
    override val siteId: String = "whatsapp"
    override val displayName: String = "WhatsApp"
    override val brandColorHex: String = "#25D366"
    override val iconName: String = "whatsapp"
    override val supportedUrlPatterns: List<String> = listOf("whatsapp.com")
}
