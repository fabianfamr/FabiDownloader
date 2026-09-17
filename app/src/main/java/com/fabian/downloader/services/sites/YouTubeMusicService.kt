package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class YouTubeMusicService : BaseSiteService() {
    override val siteId: String = "youtube_music"
    override val displayName: String = "YouTube Music"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube_music"
    override val supportedUrlPatterns: List<String> = listOf("music.youtube.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.PLAYLISTS,
        SiteCapability.EMBED_METADATA,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val supportedFormats: List<String> = listOf(
        Config.FORMAT_MP3,
        Config.FORMAT_M4A
    )

    override val supportedQualities: List<String> = listOf(
        "320kbps",
        "256kbps",
        "192kbps",
        "128kbps"
    )

    override val defaultQuality: String = "320kbps"

    override val customUserAgent: String = Config.UA_YOUTUBE_MUSIC

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
    }
}

