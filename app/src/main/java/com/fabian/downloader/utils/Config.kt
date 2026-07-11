package com.fabian.downloader.utils

object Config {
    // App Identity
    const val APP_NAME = "FabiDownloader"
    const val APP_NAME_LOWER = "fabidownloader"
    const val PACKAGE_NAME = "com.fabian.downloader"

    // GitHub
    const val GITHUB_OWNER = "fabianfamr"
    const val GITHUB_REPO = "FabiDownloader"
    const val GITHUB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"
    const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    // User-Agents
    const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    const val UA_GOOGLEBOT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
    const val UA_FACEBOOK = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
    const val UA_YOUTUBE_MUSIC = "com.google.android.youtube/19.29.37 (Linux; U; Android 14; en_US) gzip"
    const val UA_TIKTOK_MOBILE = "Mozilla/5.0 (Linux; Android 9; SM-G960F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Mobile Safari/537.36"
    const val UA_DEFAULT_CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // Endpoints
    const val PING_URL = "https://www.google.com"
    const val REFERER_DEFAULT = "https://www.google.com/"
    const val YT_OEMBED_URL = "https://www.youtube.com/oembed?url=%s&format=json"
    const val TIKTOK_OEMBED_URL = "https://www.tiktok.com/oembed?url=%s"
    const val YT_THUMBNAIL_URL = "https://img.youtube.com/vi/%s/hqdefault.jpg"

    // Files
    const val COOKIES_FILE_NAME = "cookies.txt"
}
