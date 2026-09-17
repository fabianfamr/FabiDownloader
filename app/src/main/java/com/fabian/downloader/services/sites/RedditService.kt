package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class RedditService : BaseSiteService() {
    override val siteId: String = "reddit"
    override val displayName: String = "Reddit"
    override val brandColorHex: String = "#FF4500"
    override val iconName: String = "reddit"
    override val supportedUrlPatterns: List<String> = listOf("reddit.com", "v.redd.it", "redd.it")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        // Reddit separa audio y vídeo en streams DASH diferentes: forzar multiplexión
        request.addOption("--merge-output-format", "mp4")
    }
}

