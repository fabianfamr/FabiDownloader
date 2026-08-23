package com.fabian.downloader.configs

import androidx.annotation.StringRes
import com.fabian.downloader.R

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
    const val UA_INSTAGRAM = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1"
    const val UA_DEFAULT_CHROME_WINDOWS = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // Endpoints
    const val PING_URL = "https://www.google.com"
    const val REFERER_DEFAULT = "https://www.google.com/"
    const val YT_OEMBED_URL = "https://www.youtube.com/oembed?url=%s&format=json"
    const val TIKTOK_OEMBED_URL = "https://www.tiktok.com/oembed?url=%s"
    const val YT_OEMBED_BASE_URL = "https://www.youtube.com/oembed?url="
    const val TIKTOK_OEMBED_BASE_URL = "https://www.tiktok.com/oembed?url="
    const val YT_THUMBNAIL_URL = "https://img.youtube.com/vi/{ytId}/hqdefault.jpg"

    // Files
    const val COOKIES_FILE_NAME = "cookies.txt"

    // Prefs Name
    const val PREFS_NAME = "fabi_downloader_prefs"

    // Log Tags
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
    const val TAG_APP_SETTINGS = "AppSettings"

    // Preference Keys
    const val PREF_SELECTED_QUALITY = "selectedQuality"
    const val PREF_SELECTED_VIDEO_FORMAT = "selectedVideoFormat"
    const val PREF_SELECTED_AUDIO_FORMAT = "selectedAudioFormat"
    const val PREF_NOTIFICATIONS_ENABLED = "notificationsEnabled"
    const val PREF_DATA_SAVER_ENABLED = "dataSaverEnabled"
    const val PREF_DOWNLOAD_LOCATION = "downloadLocation"
    const val PREF_MAX_SPEED = "maxSpeed"
    const val PREF_THEME_PREFERENCE = "themePreference"
    const val PREF_LANGUAGE = "language"
    const val PREF_CONFIRM_ON_DELETE = "confirmOnDelete"
    const val PREF_CONCURRENT_FRAGMENTS = "concurrentFragments"
    const val PREF_EMBED_SUBTITLES = "embedSubtitles"
    const val PREF_PLAYLIST_ENABLED = "playlistEnabled"
    const val PREF_MAX_CONCURRENT_DOWNLOADS = "maxConcurrentDownloads"
    const val PREF_EARLY_START_THRESHOLD = "earlyStartThreshold"
    const val PREF_CLIPBOARD_ACTION = "clipboardAction"
    const val PREF_LAST_DOWNLOADED_OPTION_ID = "lastDownloadedOptionId"
    const val PREF_CUSTOM_ARGUMENTS = "customArguments"
    const val PREF_COOKIES = "cookies"
    const val PREF_CUSTOM_USER_AGENT = "customUserAgent"
    const val PREF_SPONSOR_BLOCK_ENABLED = "sponsorBlockEnabled"
    const val PREF_EMBED_THUMBNAIL = "embedThumbnail"
    const val PREF_EMBED_METADATA = "embedMetadata"
    const val PREF_BYPASS_GEO = "bypassGeo"
    const val PREF_BYPASS_SSL_VERIFICATION = "bypassSslVerification"
    const val PREF_SHOW_DOWNLOAD_SPEED_IN_NOTIFICATION = "showDownloadSpeedInNotification"
    const val PREF_SELECTED_PAUSED_NOTIFICATION_TIMEOUT = "selectedPausedNotificationTimeout"
    const val PREF_BATTERY_OPTIMIZATION_ENABLED = "batteryOptimizationEnabled"
    const val PREF_SELECTED_BATTERY_LOW_THRESHOLD = "selectedBatteryLowThreshold"
    const val PREF_SELECTED_BATTERY_LOW_ACTION = "selectedBatteryLowAction"
    const val PREF_KEEP_HISTORY = "keepHistory"
    const val PREF_AUTO_RETRY = "autoRetry"
    const val PREF_DYNAMIC_COLOR = "dynamicColor"
    const val PREF_MARK_AS_MV = "markAsMV"
    const val PREF_ACCENT_COLOR_NAME = "accentColorName"
    const val PREF_SELECTED_STORAGE_MARGIN = "selectedStorageMargin"
    const val PREF_CARD_STYLE = "cardStyle"
    const val PREF_SHOW_QUALITY_BADGE = "showQualityBadge"
    const val PREF_SHOW_REALTIME_SPEED_CARD = "showRealtimeSpeedCard"
    const val PREF_DEFAULT_AUDIO_BITRATE = "defaultAudioBitrate"
    const val PREF_NOTIFY_BATCH_COMPLETE = "notifyBatchComplete"
    const val PREF_CLEAN_TEMP_ON_CANCEL = "cleanTempOnCancel"
    const val PREF_QUICK_SHARE_MODE = "quickShareMode"
    const val PREF_ALLOW_DUPLICATE_DOWNLOADS = "allowDuplicateDownloads"
    const val PREF_EMBED_CHAPTERS = "embedChapters"
    const val PREF_AMOLED_MODE = "amoledMode"

    // Default & Option Values (Technical IDs / Neutral values)
    const val QUALITY_BEST = "best"
    const val QUALITY_4K = "2160p"
    const val QUALITY_1080P = "1080p"
    const val QUALITY_720P = "720p"
    const val QUALITY_480P = "480p"
    const val QUALITY_360P = "360p"
    const val QUALITY_AUDIO_ONLY = "audio_only"
    const val DEFAULT_QUALITY = "720p"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    const val DEFAULT_LANGUAGE = "system"
    const val DEFAULT_CONCURRENT_FRAGMENTS = "4"
    const val CLIPBOARD_ACTION_BANNER = "banner"
    const val CLIPBOARD_ACTION_AUTO = "auto"
    const val CLIPBOARD_ACTION_DISABLED = "disabled"

    const val TIMEOUT_1_MIN = "1_min"
    const val TIMEOUT_5_MIN = "5_min"
    const val TIMEOUT_10_MIN = "10_min"
    const val TIMEOUT_30_MIN = "30_min"
    const val TIMEOUT_NEVER = "never"

    const val BATTERY_THRESHOLD_15 = "15%"
    const val BATTERY_THRESHOLD_20 = "20%"
    const val BATTERY_THRESHOLD_25 = "25%"
    const val BATTERY_THRESHOLD_30 = "30%"

    const val BATTERY_ACTION_OPTIMIZE = "optimize"
    const val BATTERY_ACTION_LIMIT = "limit"

    const val ACCENT_ELECTRIC_BLUE = "electric_blue"
    const val ACCENT_EMERALD_GREEN = "emerald_green"
    const val ACCENT_ROYAL_PURPLE = "royal_purple"
    const val ACCENT_SUNSET_ORANGE = "sunset_orange"
    const val ACCENT_HOT_PINK = "hot_pink"
    const val ACCENT_STEEL_GRAY = "steel_gray"

    const val STORAGE_MARGIN_DISABLED = "disabled"
    const val STORAGE_MARGIN_100MB = "100MB"
    const val STORAGE_MARGIN_250MB = "250MB"
    const val STORAGE_MARGIN_500MB = "500MB"
    const val STORAGE_MARGIN_1GB = "1GB"
    const val STORAGE_MARGIN_2GB = "2GB"
    const val STORAGE_MARGIN_3GB = "3GB"
    const val STORAGE_MARGIN_5GB = "5GB"
    const val STORAGE_MARGIN_10GB = "10GB"

    const val CARD_STYLE_DETAILED = "detailed"
    const val CARD_STYLE_COMPACT = "compact"
    const val CARD_STYLE_MINIMAL = "minimal"

    const val BITRATE_320 = "320k"
    const val BITRATE_256 = "256k"
    const val BITRATE_192 = "192k"
    const val BITRATE_128 = "128k"

    // Download Status Strings (Technical IDs)
    const val STATUS_FAILED_PREFIX = "FAILED: "
    const val STATUS_QUEUED = "QUEUED"
    const val STATUS_WAITING = "WAITING"
    const val STATUS_CONNECTING = "CONNECTING"
    const val STATUS_DOWNLOADING = "DOWNLOADING"
    const val STATUS_RETRYING = "RETRYING"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_CALCULATING = "CALCULATING"
    const val STATUS_FINALIZING = "FINALIZING"
    const val STATUS_ZERO_MB = "0 MB"

    // Fallback values for media metadata parser
    const val STATUS_UNKNOWN = "UNKNOWN"
    const val DEFAULT_TITLE = "Untitled Video"
    const val DEFAULT_AUTHOR_INSTAGRAM = "Instagram User"

    // UI Placeholder Titles
    const val TITLE_PROCESSING_LINK = "PROCESSING"
    const val TITLE_ANALYZING_SHARED = "ANALYZING"

    // Speed Options
    const val SPEED_UNLIMITED = "unlimited"
    const val SPEED_100K = "100 KB/s"
    const val SPEED_250K = "250 KB/s"
    const val SPEED_500K = "500 KB/s"
    const val SPEED_1M = "1 MB/s"
    const val SPEED_2M = "2 MB/s"
    const val SPEED_5M = "5 MB/s"
    const val SPEED_10M = "10 MB/s"
    const val SPEED_20M = "20 MB/s"
    const val SPEED_50M = "50 MB/s"

    // Media Formats
    const val FORMAT_MP4 = "MP4"
    const val FORMAT_MKV = "MKV"
    const val FORMAT_MP3 = "MP3"
    const val FORMAT_M4A = "M4A"
    const val FORMAT_WEBM = "WEBM"
    const val FORMAT_OGG = "OGG"
    const val FORMAT_WAV = "WAV"
    const val FORMAT_JPG = "JPG"
    const val FORMAT_PNG = "PNG"
    const val FORMAT_WEBP = "WEBP"

    // MIME types
    const val MIME_AUDIO = "audio/*"
    const val MIME_VIDEO = "video/*"
    const val MIME_IMAGE = "image/*"

    // Intent Extras and Actions
    const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"
    const val EXTRA_NAVIGATE_TO_DOWNLOADS = "navigate_to_downloads"
    const val EXTRA_INITIAL_PAGE = "initialPage"

    const val ACTION_OPEN = "com.fabian.downloader.ACTION_OPEN"
    const val ACTION_SHARE = "com.fabian.downloader.ACTION_SHARE"
    const val ACTION_RETRY = "com.fabian.downloader.ACTION_RETRY"
    const val ACTION_PAUSE = "com.fabian.downloader.ACTION_PAUSE"
    const val ACTION_RESUME = "com.fabian.downloader.ACTION_RESUME"
    const val ACTION_CANCEL = "com.fabian.downloader.ACTION_CANCEL"

    // Notification Channel IDs
    const val NOTIF_CHANNEL_PROGRESS = "downloads_channel_progress"
    const val NOTIF_CHANNEL_STATUS = "downloads_channel_status"
    const val NOTIF_GROUP = "downloads_group"

    // Database Names
    const val DB_NAME = "downloader-database"
    const val DB_TABLE_DOWNLOADS = "download_records"
    const val DB_TABLE_SEARCH_HISTORY = "search_history"

    // Storage Paths
    const val PATH_ROOT_FOLDER = "FabiDownloader"
    const val PATH_DB_FOLDER = "FabiDownloader/db"
    const val PATH_DOWNLOADS_FOLDER = "FabiDownloader/downloads"
    const val PATH_VIDEO_SUBFOLDER = "FabiDownloader/downloads/video"
    const val PATH_AUDIO_SUBFOLDER = "FabiDownloader/downloads/audio"
    const val PATH_IMAGE_SUBFOLDER = "FabiDownloader/downloads/image"
    const val PATH_VIDEO_SUBFOLDER_ALT = "Fabidownloader/downloads/video"
    const val PATH_AUDIO_SUBFOLDER_ALT = "Fabidownloader/downloads/audio"
    const val PATH_IMAGE_SUBFOLDER_ALT = "Fabidownloader/downloads/image"
    const val PATH_DOWNLOAD_LOCATION_DEFAULT = "FabiDownloader/downloads"

    // yt-dlp rate limit values
    const val RATE_LIMIT_100K = "100K"
    const val RATE_LIMIT_250K = "250K"
    const val RATE_LIMIT_500K = "500K"
    const val RATE_LIMIT_1M = "1M"
    const val RATE_LIMIT_2M = "2M"
    const val RATE_LIMIT_5M = "5M"
    const val RATE_LIMIT_10M = "10M"
    const val RATE_LIMIT_20M = "20M"
    const val RATE_LIMIT_50M = "50M"

    // Bot Detection Patterns
    const val BOT_DETECTION_PATTERN = "Sign in to confirm you"
    const val BOT_DETECTION_LOGIN = "login"
    @StringRes val BOT_DETECTION_PATTERN_RES = R.string.bot_detection_pattern
    @StringRes val BOT_DETECTION_LOGIN_RES = R.string.bot_detection_login

    // Valid media file extensions
    val VALID_EXTENSIONS = listOf("mp4", "mp3", "m4a", "webm", "ogg", "wav", "mkv", "jpg", "jpeg", "png", "webp")

    // Windows-reserved filenames
    val RESERVED_FILENAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )
    const val MAX_FILENAME_LENGTH = 200

    // Resource String Resolvers
    @StringRes
    fun getQualityStringRes(quality: String): Int = when (quality) {
        QUALITY_BEST -> R.string.quality_best
        QUALITY_4K -> R.string.quality_4k
        QUALITY_1080P -> R.string.quality_1080p
        QUALITY_720P -> R.string.quality_720p
        QUALITY_480P -> R.string.quality_480p
        QUALITY_360P -> R.string.quality_360p
        QUALITY_AUDIO_ONLY -> R.string.quality_audio_only
        else -> R.string.quality_best
    }

    @StringRes
    fun getThemeStringRes(theme: String): Int = when (theme) {
        THEME_LIGHT -> R.string.theme_light
        THEME_DARK -> R.string.theme_dark
        else -> R.string.theme_system
    }

    @StringRes
    fun getCardStyleStringRes(style: String): Int = when (style) {
        CARD_STYLE_COMPACT -> R.string.card_style_compact
        CARD_STYLE_MINIMAL -> R.string.card_style_minimal
        else -> R.string.card_style_detailed
    }

    @StringRes
    fun getAudioBitrateStringRes(bitrate: String): Int = when (bitrate) {
        BITRATE_320 -> R.string.audio_bitrate_320
        BITRATE_256 -> R.string.audio_bitrate_256
        BITRATE_128 -> R.string.audio_bitrate_128
        else -> R.string.audio_bitrate_192
    }

    @StringRes
    fun getTimeoutStringRes(timeout: String): Int = when (timeout) {
        TIMEOUT_1_MIN -> R.string.timeout_1_min
        TIMEOUT_5_MIN -> R.string.timeout_5_min
        TIMEOUT_30_MIN -> R.string.timeout_30_min
        TIMEOUT_NEVER -> R.string.timeout_never
        else -> R.string.timeout_10_min
    }

    @StringRes
    fun getBatteryActionStringRes(action: String): Int = when (action) {
        BATTERY_ACTION_LIMIT -> R.string.battery_action_limit
        else -> R.string.battery_action_optimize
    }

    @StringRes
    fun getAccentColorStringRes(accent: String): Int = when (accent) {
        ACCENT_EMERALD_GREEN -> R.string.accent_emerald_green
        ACCENT_ROYAL_PURPLE -> R.string.accent_royal_purple
        ACCENT_SUNSET_ORANGE -> R.string.accent_sunset_orange
        ACCENT_HOT_PINK -> R.string.accent_hot_pink
        ACCENT_STEEL_GRAY -> R.string.accent_steel_gray
        else -> R.string.accent_electric_blue
    }

    @StringRes
    fun getStorageMarginStringRes(margin: String): Int = when (margin) {
        STORAGE_MARGIN_DISABLED -> R.string.storage_margin_disabled
        STORAGE_MARGIN_100MB -> R.string.storage_margin_100mb
        STORAGE_MARGIN_250MB -> R.string.storage_margin_250mb
        STORAGE_MARGIN_1GB -> R.string.storage_margin_1gb
        STORAGE_MARGIN_2GB -> R.string.storage_margin_2gb
        STORAGE_MARGIN_3GB -> R.string.storage_margin_3gb
        STORAGE_MARGIN_5GB -> R.string.storage_margin_5gb
        STORAGE_MARGIN_10GB -> R.string.storage_margin_10gb
        else -> R.string.storage_margin_500mb
    }

    @StringRes
    fun getSpeedStringRes(speed: String): Int = when (speed) {
        SPEED_100K -> R.string.speed_100k
        SPEED_250K -> R.string.speed_250k
        SPEED_500K -> R.string.speed_500k
        SPEED_1M -> R.string.speed_1m
        SPEED_2M -> R.string.speed_2m
        SPEED_5M -> R.string.speed_5m
        SPEED_10M -> R.string.speed_10m
        SPEED_20M -> R.string.speed_20m
        SPEED_50M -> R.string.speed_50m
        else -> R.string.speed_unlimited
    }
}
