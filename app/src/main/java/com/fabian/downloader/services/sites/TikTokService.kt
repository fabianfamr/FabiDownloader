package com.fabian.downloader.services.sites

import com.fabian.downloader.configs.Config
import com.yausername.youtubedl_android.YoutubeDLRequest

class TikTokService : BaseSiteService() {
    override val siteId: String = "tiktok"
    override val displayName: String = "TikTok"
    override val brandColorHex: String = "#00F2FE"
    override val iconName: String = "tiktok"
    override val supportedUrlPatterns: List<String> = listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com", "douyin.com")

    override val capabilities: Set<SiteCapability> = setOf(
        SiteCapability.AUDIO_EXTRACTION,
        SiteCapability.VIDEO_MULTI_QUALITY,
        SiteCapability.WATERMARK_REMOVAL,
        SiteCapability.CUSTOM_USER_AGENT,
        SiteCapability.FAST_HTTP_STREAM
    )

    override val customUserAgent: String = Config.UA_TIKTOK_MOBILE
    override val customReferer: String = "https://www.tiktok.com/"

    override fun cleanUrl(url: String): String {
        return com.fabian.downloader.utils.UrlUtils.cleanTrackingParams(url)
    }

    override fun customizeExtractorRequest(request: YoutubeDLRequest, url: String) {
        super.customizeExtractorRequest(request, url)
        request.addOption("--extractor-args", "tiktok:api_hostname=api22-normal-c-useast1a.tiktokv.com")
    }

    override fun customizeDownloaderRequest(request: YoutubeDLRequest, url: String) {
        super.customizeDownloaderRequest(request, url)
        request.addOption("--extractor-args", "tiktok:api_hostname=api22-normal-c-useast1a.tiktokv.com")
    }
}

