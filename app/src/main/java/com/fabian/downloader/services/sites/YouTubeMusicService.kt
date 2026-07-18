package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class YouTubeMusicService : BaseSiteService() {
    override val siteId: String = "youtube_music"
    override val displayName: String = "YouTube Music"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube_music"
    override val supportedUrlPatterns: List<String> = listOf("music.youtube.com")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,android")
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_YOUTUBE_MUSIC)
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
        request.addOption("--no-check-formats")
        request.addOption("--youtube-skip-dash-manifest")
        request.addOption("--youtube-skip-hls-manifest")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,android")
        request.addOption("--user-agent", com.fabian.downloader.utils.Config.UA_YOUTUBE_MUSIC)
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
        request.addOption("--no-check-formats")
        request.addOption("--youtube-skip-dash-manifest")
        request.addOption("--youtube-skip-hls-manifest")
    }
}
