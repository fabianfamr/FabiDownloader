package com.fabian.downloader.services.sites

object SiteServiceProvider {
    val services: List<SiteService> = listOf(
        YouTubeMusicService(),
        YouTubeService(),
        InstagramService(),
        TikTokService(),
        FacebookService(),
        TwitterService(),
        TwitchService(),
        KickService()
    )

    fun getServiceForUrl(url: String): SiteService {
        return services.find { it.canHandle(url) } ?: GenericSiteService()
    }
}
