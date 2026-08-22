package com.fabian.downloader.services.sites

class DiscordService : BaseSiteService() {
    override val siteId: String = "discord"
    override val displayName: String = "Discord"
    override val brandColorHex: String = "#5865F2"
    override val iconName: String = "discord"
    override val supportedUrlPatterns: List<String> = listOf("discord.com", "discordapp.com")
}
