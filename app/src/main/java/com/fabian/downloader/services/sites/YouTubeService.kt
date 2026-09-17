package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class YouTubeService : BaseSiteService() {
    override val siteId: String = "youtube"
    override val displayName: String = "YouTube"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube"
    override val supportedUrlPatterns: List<String> = listOf("youtube.com", "youtu.be", "m.youtube.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.PLAYLISTS,
        SiteCapability.SUBTITLES,
        SiteCapability.EMBED_METADATA,
        SiteCapability.FAST_HTTP_STREAM
    )

    override val supportedQualities: List<String> = listOf(
        Config.QUALITY_BEST,
        "1080p",
        "720p",
        "480p",
        "360p",
        "240p",
        "144p"
    )

    override val defaultQuality: String = "720p"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--extractor-args", "youtube:player_skip=configs")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player_skip=configs")
    }
}

