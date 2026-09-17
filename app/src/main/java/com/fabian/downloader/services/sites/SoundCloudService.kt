package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class SoundCloudService : BaseSiteService() {
    override val siteId: String = "soundcloud"
    override val displayName: String = "SoundCloud"
    override val brandColorHex: String = "#FF5500"
    override val iconName: String = "soundcloud"
    override val supportedUrlPatterns: List<String> = listOf("soundcloud.com", "on.soundcloud.com", "m.soundcloud.com")
    
    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.PLAYLISTS,
        SiteCapability.EMBED_METADATA,
        SiteCapability.CUSTOM_USER_AGENT
    )

    override val supportedFormats: List<String> = listOf(Config.FORMAT_MP3, Config.FORMAT_M4A)
    override val supportedQualities: List<String> = listOf("320kbps", "256kbps", "192kbps", "128kbps")
    override val defaultQuality: String = "320kbps"

    override val customUserAgent: String = Config.UA_DEFAULT_CHROME_WINDOWS
    override val customReferer: String = "https://soundcloud.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--audio-quality", "0")
    }
}

