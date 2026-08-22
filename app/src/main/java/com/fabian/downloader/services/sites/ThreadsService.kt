package com.fabian.downloader.services.sites

class ThreadsService : BaseSiteService() {
    override val siteId: String = "threads"
    override val displayName: String = "Threads"
    override val brandColorHex: String = "#000000"
    override val iconName: String = "threads"
    override val supportedUrlPatterns: List<String> = listOf("threads.net")
}
