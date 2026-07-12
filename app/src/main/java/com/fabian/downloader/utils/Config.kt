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

    // =============================================================
    // Log Tags
    // =============================================================
    const val TAG_DOWNLOAD_MANAGER = "DownloadManager"
    const val TAG_EXTRACTION_SERVICE = "ExtractionService"
    const val TAG_YTDLP_DOWNLOADER = "YtdlpDownloader"
    const val TAG_YTDLP_EXTRACTOR = "YtdlpExtractor"
    const val TAG_BASE_SITE_SERVICE = "BaseSiteService"
    const val TAG_DOWNLOAD_ACTION_RECEIVER = "DownloadActionReceiver"
    const val TAG_SHARE_POPUP_SCREEN = "SharePopupScreen"
    const val TAG_UPDATE_MANAGER = "UpdateManager"
    const val TAG_PATH_UTILS = "PathUtils"
    const val TAG_YT_DLP = "yt-dlp"

    // =============================================================
    // Download Status Strings (persisted in DB, compared with ==/startsWith)
    // IMPORTANT: Do NOT localize these — they are stored in the database
    // and used as markers for state machine logic.
    // =============================================================
    const val STATUS_FAILED_PREFIX = "Fallo: "
    const val STATUS_QUEUED = "En cola..."
    const val STATUS_WAITING = "Esperando..."
    const val STATUS_CONNECTING = "Conectando..."
    const val STATUS_DOWNLOADING = "Descargando..."
    const val STATUS_COMPLETED = "Completado"
    const val STATUS_CALCULATING = "Calculando..."
    const val STATUS_FINALIZING = "Finalizando..."
    const val STATUS_ZERO_MB = "0 MB"

    // Fallback values for media metadata parser (compared with == in ExtractionService)
    const val STATUS_UNKNOWN = "Desconocido"
    const val DEFAULT_TITLE = "Video sin título"
    const val DEFAULT_AUTHOR_INSTAGRAM = "Instagram User"

    // =============================================================
    // Media Formats (technical identifiers, not user-facing)
    // =============================================================
    const val FORMAT_MP4 = "MP4"
    const val FORMAT_MP3 = "MP3"
    const val FORMAT_M4A = "M4A"
    const val FORMAT_WEBM = "WEBM"
    const val FORMAT_OGG = "OGG"
    const val FORMAT_WAV = "WAV"

    // MIME types
    const val MIME_AUDIO = "audio/*"
    const val MIME_VIDEO = "video/*"

    // =============================================================
    // Intent Extras and Actions
    // =============================================================
    const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"
    const val EXTRA_NAVIGATE_TO_DOWNLOADS = "navigate_to_downloads"
    const val EXTRA_INITIAL_PAGE = "initialPage"

    const val ACTION_OPEN = "com.fabian.downloader.ACTION_OPEN"
    const val ACTION_SHARE = "com.fabian.downloader.ACTION_SHARE"
    const val ACTION_RETRY = "com.fabian.downloader.ACTION_RETRY"
    const val ACTION_PAUSE = "com.fabian.downloader.ACTION_PAUSE"

    // =============================================================
    // Notification Channel IDs
    // =============================================================
    const val NOTIF_CHANNEL_PROGRESS = "downloads_channel_progress"
    const val NOTIF_CHANNEL_STATUS = "downloads_channel_status"
    const val NOTIF_GROUP = "downloads_group"

    // =============================================================
    // Database Names
    // =============================================================
    const val DB_NAME = "downloader-database"
    const val DB_TABLE_DOWNLOADS = "download_records"
    const val DB_TABLE_SEARCH_HISTORY = "search_history"

    // =============================================================
    // Storage Paths
    // =============================================================
    const val PATH_VIDEO_SUBFOLDER = "FabiDownloader/video"
    const val PATH_AUDIO_SUBFOLDER = "FabiDownloader/audio"
    const val PATH_VIDEO_SUBFOLDER_ALT = "Fabidownloader/video"
    const val PATH_AUDIO_SUBFOLDER_ALT = "Fabidownloader/audio"
    const val PATH_DOWNLOAD_LOCATION_DEFAULT = "Downloads/FabiDownloader"

    // =============================================================
    // UI Placeholder Titles (compared with ==, used as state markers)
    // =============================================================
    const val TITLE_PROCESSING_LINK = "Procesando enlace..."
    const val TITLE_ANALYZING_SHARED = "Analizando enlace compartido..."

    // =============================================================
    // Speed Options (used in AppSettings + YtdlpDownloader)
    // =============================================================
    const val SPEED_UNLIMITED = "Ilimitada"
    const val SPEED_500K = "500 KB/s"
    const val SPEED_1M = "1 MB/s"
    const val SPEED_5M = "5 MB/s"
    const val SPEED_10M = "10 MB/s"

    // yt-dlp rate limit values
    const val RATE_LIMIT_500K = "500K"
    const val RATE_LIMIT_1M = "1M"
    const val RATE_LIMIT_5M = "5M"
    const val RATE_LIMIT_10M = "10M"

    // =============================================================
    // Bot Detection Patterns (yt-dlp error messages)
    // =============================================================
    const val BOT_DETECTION_PATTERN = "Sign in to confirm you"
    const val BOT_DETECTION_LOGIN = "login"

    // =============================================================
    // Valid media file extensions (used to find downloaded file)
    // =============================================================
    val VALID_EXTENSIONS = listOf("mp4", "mp3", "m4a", "webm", "ogg", "wav", "mkv")

    // Windows-reserved filenames (sanitization safety, for cloud sync)
    val RESERVED_FILENAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    const val MAX_FILENAME_LENGTH = 200
}
