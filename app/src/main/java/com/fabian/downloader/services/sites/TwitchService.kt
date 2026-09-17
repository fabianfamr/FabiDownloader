package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class TwitchService : BaseSiteService() {
    override val siteId: String = "twitch"
    override val displayName: String = "Twitch"
    override val brandColorHex: String = "#9146FF"
    override val iconName: String = "twitch"
    override val supportedUrlPatterns: List<String> = listOf("twitch.tv", "clips.twitch.tv")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://www.twitch.tv/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--hls-prefer-native")
    }
}

