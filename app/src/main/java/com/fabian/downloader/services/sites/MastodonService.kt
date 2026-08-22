package com.fabian.downloader.services.sites

class MastodonService : BaseSiteService() {
    override val siteId: String = "mastodon"
    override val displayName: String = "Mastodon"
    override val brandColorHex: String = "#6364FF"
    override val iconName: String = "mastodon"
    override val supportedUrlPatterns: List<String> = listOf("mastodon.social", "joinmastodon.org")
}
