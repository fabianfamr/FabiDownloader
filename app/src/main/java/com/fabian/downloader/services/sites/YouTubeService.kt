package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class YouTubeService : BaseSiteService() {
    override val siteId: String = "youtube"
    override val displayName: String = "YouTube"
    override val brandColorHex: String = "#FF0000"
    override val iconName: String = "youtube"
    override val supportedUrlPatterns: List<String> = listOf("youtube.com", "youtu.be", "shorts")

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,android,web")
        
        val customUa = com.fabian.downloader.ui.AppSettings.customUserAgent
        if (customUa.isNotEmpty()) {
            request.addOption("--user-agent", customUa)
        } else {
            request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
        }
        
        request.addOption("--no-check-certificate")
        request.addOption("--no-check-formats")
        request.addOption("--youtube-skip-dash-manifest")
        request.addOption("--youtube-skip-hls-manifest")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "youtube:player-client=ios,android,web")
        
        val customUa = com.fabian.downloader.ui.AppSettings.customUserAgent
        if (customUa.isNotEmpty()) {
            request.addOption("--user-agent", customUa)
        } else {
            request.addOption("--user-agent", Config.UA_DEFAULT_CHROME_WINDOWS)
        }
        
        request.addOption("--no-check-certificate")
        request.addOption("--no-check-formats")
        request.addOption("--youtube-skip-dash-manifest")
        request.addOption("--youtube-skip-hls-manifest")
    }
}
