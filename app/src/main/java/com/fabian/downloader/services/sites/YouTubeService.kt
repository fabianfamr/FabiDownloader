package com.fabian.downloader.services.sites

import com.yausername.youtubedl_android.YoutubeDLRequest

class YouTubeService : BaseSiteService() {
    override val siteId: String = "youtube"
    override val displayName: String = "YouTube"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube"
    override val supportedUrlPatterns: List<String> = listOf("youtube.com", "youtu.be", "shorts")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,web,android")
        request.addOption("--user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")
        request.addOption("--no-check-certificate")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,web,android")
        request.addOption("--user-agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")
        request.addOption("--no-check-certificate")
    }
}
