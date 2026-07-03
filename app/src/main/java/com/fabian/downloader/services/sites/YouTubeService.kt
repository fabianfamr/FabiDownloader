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
        request.addOption("--extractor-args", "youtube:player-client=android,web,ios")
        request.addOption("--user-agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip")
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=android,web,ios")
        request.addOption("--user-agent", "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip")
        request.addOption("--add-header", "X-Youtube-Client-Name: 3")
        request.addOption("--add-header", "X-Youtube-Client-Version: 19.29.37")
    }
}
