package com.fabian.downloader.services.sites

object SiteServiceProvider {
    val services: List<SiteService> by lazy {
        listOf(
            YouTubeMusicService(),
            YouTubeService(),
            InstagramService(),
            TikTokService(),
            FacebookService(),
            TwitterService(),
            TwitchService(),
            KickService(),
            RedditService(),
            PinterestService(),
            VimeoService(),
            SoundCloudService()
        )
    }

    fun getServiceForUrl(url: String): SiteService {
        val cleanUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url)
        return services.find { it.canHandle(cleanUrl) } ?: GenericSiteService()
    }
}
