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
            SoundCloudService(),
            DailymotionService(),
            BilibiliService(),
            TumblrService(),
            VKService(),
            RumbleService(),
            SnapchatService(),
            ThreadsService(),
            PatreonService(),
            BandcampService(),
            MixcloudService(),
            DropboxService(),
            GoogleDriveService(),
            TelegramService(),
            WhatsAppService(),
            DiscordService(),
            ImgurService(),
            FlickrService(),
            GiphyService(),
            AppleMusicService(),
            OdyseeService(),
            MastodonService(),
            SpotifyService()
        )
    }

    fun getServiceForUrl(url: String): SiteService {
        val cleanUrl = com.fabian.downloader.pipeline.DownloadAssemblyLine.station1_cleanUrl(url)
        return services.find { it.canHandle(cleanUrl) } ?: GenericSiteService()
    }
}
