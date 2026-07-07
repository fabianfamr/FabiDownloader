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
        request.addOption("--extractor-args", "youtube:player-client=android,web")
        
        val customUa = com.fabian.downloader.ui.AppSettings.customUserAgent
        if (customUa.isNotEmpty()) {
            request.addOption("--user-agent", customUa)
        } else {
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        }
        
        request.addOption("--no-check-certificate")
        request.addOption("--no-check-formats")
        request.addOption("--youtube-skip-dash-manifest")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=android,web")
        
        val customUa = com.fabian.downloader.ui.AppSettings.customUserAgent
        if (customUa.isNotEmpty()) {
            request.addOption("--user-agent", customUa)
        } else {
            request.addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
        }
        
        request.addOption("--no-check-certificate")
    }
}
