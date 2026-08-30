package com.fabian.downloader.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.fabian.downloader.configs.Config
import com.fabian.downloader.utils.PathUtils
import java.io.File

object AppSettings {
    private lateinit var prefs: SharedPreferences
    private val listeners = mutableListOf<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) listeners.add(listener)
        }
    }

    fun removeListener(listener: (String) -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    private fun notifyChanged(key: String) {
        val targets = synchronized(listeners) { listeners.toList() }
        targets.forEach { listener ->
            try { listener(key) } catch (e: Exception) {
                Log.e(Config.TAG_APP_SETTINGS, "Error en listener para clave $key", e)
            }
        }
    }

    // Listas delegadas a AppSettingsDefaults
    val qualityOptions get() = AppSettingsDefaults.qualityOptions
    val videoFormats get() = AppSettingsDefaults.videoFormats
    val audioFormats get() = AppSettingsDefaults.audioFormats
    val themeOptions get() = AppSettingsDefaults.themeOptions
    val speedOptions get() = AppSettingsDefaults.speedOptions
    val cardStyleOptions get() = AppSettingsDefaults.cardStyleOptions
    val defaultAudioBitrateOptions get() = AppSettingsDefaults.defaultAudioBitrateOptions
    val pausedNotificationTimeoutOptions get() = AppSettingsDefaults.pausedNotificationTimeoutOptions
    val batteryLowThresholdOptions get() = AppSettingsDefaults.batteryLowThresholdOptions
    val batteryLowActionOptions get() = AppSettingsDefaults.batteryLowActionOptions
    val accentColorOptions get() = AppSettingsDefaults.accentColorOptions
    val storageMarginOptions get() = AppSettingsDefaults.storageMarginOptions

    // Preferencias de Calidad y Formato
    private val _selectedQuality = mutableStateOf(Config.DEFAULT_QUALITY)
    var selectedQuality: String
        get() = _selectedQuality.value
        set(value) { if (_selectedQuality.value != value) { _selectedQuality.value = value; saveString(Config.PREF_SELECTED_QUALITY, value); notifyChanged(Config.PREF_SELECTED_QUALITY) } }

    private val _selectedVideoFormat = mutableStateOf(Config.FORMAT_MP4)
    var selectedVideoFormat: String
        get() = _selectedVideoFormat.value
        set(value) { if (_selectedVideoFormat.value != value) { _selectedVideoFormat.value = value; saveString(Config.PREF_SELECTED_VIDEO_FORMAT, value); notifyChanged(Config.PREF_SELECTED_VIDEO_FORMAT) } }

    private val _selectedAudioFormat = mutableStateOf(Config.FORMAT_MP3)
    var selectedAudioFormat: String
        get() = _selectedAudioFormat.value
        set(value) { if (_selectedAudioFormat.value != value) { _selectedAudioFormat.value = value; saveString(Config.PREF_SELECTED_AUDIO_FORMAT, value); notifyChanged(Config.PREF_SELECTED_AUDIO_FORMAT) } }

    private val _defaultAudioBitrate = mutableStateOf(Config.BITRATE_192)
    var defaultAudioBitrate: String
        get() = _defaultAudioBitrate.value
        set(value) { if (_defaultAudioBitrate.value != value) { _defaultAudioBitrate.value = value; saveString(Config.PREF_DEFAULT_AUDIO_BITRATE, value); notifyChanged(Config.PREF_DEFAULT_AUDIO_BITRATE) } }

    // Almacenamiento y Velocidad
    private val _downloadLocation = mutableStateOf(Config.PATH_DOWNLOAD_LOCATION_DEFAULT)
    var downloadLocation: String
        get() = _downloadLocation.value
        set(value) { if (_downloadLocation.value != value) { _downloadLocation.value = value; PathUtils.clearFolderCache(); saveString(Config.PREF_DOWNLOAD_LOCATION, value); notifyChanged(Config.PREF_DOWNLOAD_LOCATION) } }

    private val _maxSpeed = mutableStateOf(Config.SPEED_UNLIMITED)
    var maxSpeed: String
        get() = _maxSpeed.value
        set(value) { if (_maxSpeed.value != value) { _maxSpeed.value = value; saveString(Config.PREF_MAX_SPEED, value); notifyChanged(Config.PREF_MAX_SPEED) } }

    private val _selectedStorageMargin = mutableStateOf(Config.STORAGE_MARGIN_500MB)
    val selectedStorageMarginState: State<String> get() = _selectedStorageMargin
    var selectedStorageMargin: String
        get() = _selectedStorageMargin.value
        set(value) { _selectedStorageMargin.value = value; saveString(Config.PREF_SELECTED_STORAGE_MARGIN, value); notifyChanged(Config.PREF_SELECTED_STORAGE_MARGIN) }

    val storageMarginBytes: Long get() = AppSettingsDefaults.calculateStorageMarginBytes(selectedStorageMargin)

    // Motor yt-dlp y Concurrencia
    private val _concurrentFragments = mutableStateOf(Config.DEFAULT_CONCURRENT_FRAGMENTS)
    var concurrentFragments: String
        get() = _concurrentFragments.value
        set(value) { if (_concurrentFragments.value != value) { _concurrentFragments.value = value; saveString(Config.PREF_CONCURRENT_FRAGMENTS, value); notifyChanged(Config.PREF_CONCURRENT_FRAGMENTS) } }

    private val _maxConcurrentDownloads = mutableStateOf(2)
    var maxConcurrentDownloads: Int
        get() = _maxConcurrentDownloads.value
        set(value) { if (_maxConcurrentDownloads.value != value) { _maxConcurrentDownloads.value = value; if (::prefs.isInitialized) prefs.edit { putInt(Config.PREF_MAX_CONCURRENT_DOWNLOADS, value) }; notifyChanged(Config.PREF_MAX_CONCURRENT_DOWNLOADS) } }

    private val _earlyStartThreshold = mutableStateOf(0)
    var earlyStartThreshold: Int
        get() = _earlyStartThreshold.value
        set(value) { if (_earlyStartThreshold.value != value) { _earlyStartThreshold.value = value; if (::prefs.isInitialized) prefs.edit { putInt(Config.PREF_EARLY_START_THRESHOLD, value) }; notifyChanged(Config.PREF_EARLY_START_THRESHOLD) } }

    private val _customArguments = mutableStateOf("")
    var customArguments: String
        get() = _customArguments.value
        set(value) { if (_customArguments.value != value) { _customArguments.value = value; saveString(Config.PREF_CUSTOM_ARGUMENTS, value); notifyChanged(Config.PREF_CUSTOM_ARGUMENTS) } }

    private val _customUserAgent = mutableStateOf("")
    var customUserAgent: String
        get() = _customUserAgent.value
        set(value) { if (_customUserAgent.value != value) { _customUserAgent.value = value; saveString(Config.PREF_CUSTOM_USER_AGENT, value); notifyChanged(Config.PREF_CUSTOM_USER_AGENT) } }

    private val _cookies = mutableStateOf("")
    var cookies: String
        get() = _cookies.value
        set(value) { if (_cookies.value != value) { _cookies.value = value; saveString(Config.PREF_COOKIES, value); syncCookiesFile(com.fabian.downloader.MyApplication.getInstance(), value); notifyChanged(Config.PREF_COOKIES) } }

    private fun syncCookiesFile(context: Context, cookieContent: String) {
        try {
            val cookiesFile = File(context.filesDir, Config.COOKIES_FILE_NAME)
            if (cookieContent.trim().isEmpty()) { if (cookiesFile.exists()) cookiesFile.delete() }
            else { cookiesFile.writeText(cookieContent) }
        } catch (e: Exception) { Log.e(Config.TAG_APP_SETTINGS, "Error syncing cookies.txt", e) }
    }

    // Opciones y Flags de Integración
    private val _notificationsEnabled = mutableStateOf(true)
    var notificationsEnabled: Boolean
        get() = _notificationsEnabled.value
        set(value) { if (_notificationsEnabled.value != value) { _notificationsEnabled.value = value; saveBoolean(Config.PREF_NOTIFICATIONS_ENABLED, value); notifyChanged(Config.PREF_NOTIFICATIONS_ENABLED) } }

    private val _dataSaverEnabled = mutableStateOf(false)
    var dataSaverEnabled: Boolean
        get() = _dataSaverEnabled.value
        set(value) { if (_dataSaverEnabled.value != value) { _dataSaverEnabled.value = value; saveBoolean(Config.PREF_DATA_SAVER_ENABLED, value); notifyChanged(Config.PREF_DATA_SAVER_ENABLED) } }

    private val _confirmOnDelete = mutableStateOf(true)
    var confirmOnDelete: Boolean
        get() = _confirmOnDelete.value
        set(value) { _confirmOnDelete.value = value; saveBoolean(Config.PREF_CONFIRM_ON_DELETE, value) }

    private val _embedSubtitles = mutableStateOf(false)
    var embedSubtitles: Boolean
        get() = _embedSubtitles.value
        set(value) { if (_embedSubtitles.value != value) { _embedSubtitles.value = value; saveBoolean(Config.PREF_EMBED_SUBTITLES, value); notifyChanged(Config.PREF_EMBED_SUBTITLES) } }

    private val _playlistEnabled = mutableStateOf(false)
    var playlistEnabled: Boolean
        get() = _playlistEnabled.value
        set(value) { _playlistEnabled.value = value; saveBoolean(Config.PREF_PLAYLIST_ENABLED, value) }

    private val _clipboardAction = mutableStateOf(Config.CLIPBOARD_ACTION_BANNER)
    var clipboardAction: String
        get() = _clipboardAction.value
        set(value) { _clipboardAction.value = value; saveString(Config.PREF_CLIPBOARD_ACTION, value) }

    private val _lastDownloadedOptionId = mutableStateOf("")
    var lastDownloadedOptionId: String
        get() = _lastDownloadedOptionId.value
        set(value) { _lastDownloadedOptionId.value = value; saveString(Config.PREF_LAST_DOWNLOADED_OPTION_ID, value) }

    private val _sponsorBlockEnabled = mutableStateOf(false)
    var sponsorBlockEnabled: Boolean
        get() = _sponsorBlockEnabled.value
        set(value) { if (_sponsorBlockEnabled.value != value) { _sponsorBlockEnabled.value = value; saveBoolean(Config.PREF_SPONSOR_BLOCK_ENABLED, value); notifyChanged(Config.PREF_SPONSOR_BLOCK_ENABLED) } }

    private val _embedThumbnail = mutableStateOf(true)
    var embedThumbnail: Boolean
        get() = _embedThumbnail.value
        set(value) { if (_embedThumbnail.value != value) { _embedThumbnail.value = value; saveBoolean(Config.PREF_EMBED_THUMBNAIL, value); notifyChanged(Config.PREF_EMBED_THUMBNAIL) } }

    private val _embedMetadata = mutableStateOf(true)
    var embedMetadata: Boolean
        get() = _embedMetadata.value
        set(value) { if (_embedMetadata.value != value) { _embedMetadata.value = value; saveBoolean(Config.PREF_EMBED_METADATA, value); notifyChanged(Config.PREF_EMBED_METADATA) } }

    private val _bypassGeo = mutableStateOf(true)
    var bypassGeo: Boolean
        get() = _bypassGeo.value
        set(value) { if (_bypassGeo.value != value) { _bypassGeo.value = value; saveBoolean(Config.PREF_BYPASS_GEO, value); notifyChanged(Config.PREF_BYPASS_GEO) } }

    private val _bypassSslVerification = mutableStateOf(false)
    var bypassSslVerification: Boolean
        get() = _bypassSslVerification.value
        set(value) { if (_bypassSslVerification.value != value) { _bypassSslVerification.value = value; saveBoolean(Config.PREF_BYPASS_SSL_VERIFICATION, value); notifyChanged(Config.PREF_BYPASS_SSL_VERIFICATION) } }

    private val _showDownloadSpeedInNotification = mutableStateOf(true)
    var showDownloadSpeedInNotification: Boolean
        get() = _showDownloadSpeedInNotification.value
        set(value) { _showDownloadSpeedInNotification.value = value; saveBoolean(Config.PREF_SHOW_DOWNLOAD_SPEED_IN_NOTIFICATION, value) }

    private val _selectedPausedNotificationTimeout = mutableStateOf(Config.TIMEOUT_10_MIN)
    val selectedPausedNotificationTimeoutState: State<String> get() = _selectedPausedNotificationTimeout
    var selectedPausedNotificationTimeout: String
        get() = _selectedPausedNotificationTimeout.value
        set(value) { _selectedPausedNotificationTimeout.value = value; saveString(Config.PREF_SELECTED_PAUSED_NOTIFICATION_TIMEOUT, value); notifyChanged(Config.PREF_SELECTED_PAUSED_NOTIFICATION_TIMEOUT) }

    val pausedNotificationTimeoutMs: Long get() = AppSettingsDefaults.calculatePausedTimeoutMs(selectedPausedNotificationTimeout)

    private val _batteryOptimizationEnabled = mutableStateOf(true)
    var batteryOptimizationEnabled: Boolean
        get() = _batteryOptimizationEnabled.value
        set(value) { _batteryOptimizationEnabled.value = value; saveBoolean(Config.PREF_BATTERY_OPTIMIZATION_ENABLED, value); notifyChanged(Config.PREF_BATTERY_OPTIMIZATION_ENABLED) }

    private val _selectedBatteryLowThreshold = mutableStateOf(Config.BATTERY_THRESHOLD_20)
    val selectedBatteryLowThresholdState: State<String> get() = _selectedBatteryLowThreshold
    var selectedBatteryLowThreshold: String
        get() = _selectedBatteryLowThreshold.value
        set(value) { _selectedBatteryLowThreshold.value = value; saveString(Config.PREF_SELECTED_BATTERY_LOW_THRESHOLD, value); notifyChanged(Config.PREF_SELECTED_BATTERY_LOW_THRESHOLD) }

    val batteryLowThresholdInt: Int get() = selectedBatteryLowThreshold.replace("%", "").toIntOrNull() ?: 20

    private val _selectedBatteryLowAction = mutableStateOf(Config.BATTERY_ACTION_OPTIMIZE)
    val selectedBatteryLowActionState: State<String> get() = _selectedBatteryLowAction
    var selectedBatteryLowAction: String
        get() = _selectedBatteryLowAction.value
        set(value) { _selectedBatteryLowAction.value = value; saveString(Config.PREF_SELECTED_BATTERY_LOW_ACTION, value); notifyChanged(Config.PREF_SELECTED_BATTERY_LOW_ACTION) }

    val batteryLowAction: String get() = selectedBatteryLowAction

    private val _keepHistory = mutableStateOf(true)
    var keepHistory: Boolean
        get() = _keepHistory.value
        set(value) { _keepHistory.value = value; saveBoolean(Config.PREF_KEEP_HISTORY, value) }

    private val _autoRetry = mutableStateOf(false)
    var autoRetry: Boolean
        get() = _autoRetry.value
        set(value) { if (_autoRetry.value != value) { _autoRetry.value = value; saveBoolean(Config.PREF_AUTO_RETRY, value); notifyChanged(Config.PREF_AUTO_RETRY) } }

    private val _notifyBatchComplete = mutableStateOf(true)
    var notifyBatchComplete: Boolean
        get() = _notifyBatchComplete.value
        set(value) { if (_notifyBatchComplete.value != value) { _notifyBatchComplete.value = value; saveBoolean(Config.PREF_NOTIFY_BATCH_COMPLETE, value); notifyChanged(Config.PREF_NOTIFY_BATCH_COMPLETE) } }

    private val _cleanTempOnCancel = mutableStateOf(true)
    var cleanTempOnCancel: Boolean
        get() = _cleanTempOnCancel.value
        set(value) { if (_cleanTempOnCancel.value != value) { _cleanTempOnCancel.value = value; saveBoolean(Config.PREF_CLEAN_TEMP_ON_CANCEL, value); notifyChanged(Config.PREF_CLEAN_TEMP_ON_CANCEL) } }

    private val _quickShareMode = mutableStateOf(true)
    val quickShareModeState: State<Boolean> get() = _quickShareMode
    var quickShareMode: Boolean
        get() = _quickShareMode.value
        set(value) { if (_quickShareMode.value != value) { _quickShareMode.value = value; saveBoolean(Config.PREF_QUICK_SHARE_MODE, value); notifyChanged(Config.PREF_QUICK_SHARE_MODE) } }

    private val _allowDuplicateDownloads = mutableStateOf(true)
    var allowDuplicateDownloads: Boolean
        get() = _allowDuplicateDownloads.value
        set(value) { if (_allowDuplicateDownloads.value != value) { _allowDuplicateDownloads.value = value; saveBoolean(Config.PREF_ALLOW_DUPLICATE_DOWNLOADS, value); notifyChanged(Config.PREF_ALLOW_DUPLICATE_DOWNLOADS) } }

    private val _embedChapters = mutableStateOf(true)
    var embedChapters: Boolean
        get() = _embedChapters.value
        set(value) { if (_embedChapters.value != value) { _embedChapters.value = value; saveBoolean(Config.PREF_EMBED_CHAPTERS, value); notifyChanged(Config.PREF_EMBED_CHAPTERS) } }

    // Apariencia y UI
    private val _themePreference = mutableStateOf(Config.THEME_SYSTEM)
    val themePreferenceState: State<String> get() = _themePreference
    var themePreference: String
        get() = _themePreference.value
        set(value) { if (_themePreference.value != value) { _themePreference.value = value; saveString(Config.PREF_THEME_PREFERENCE, value); notifyChanged(Config.PREF_THEME_PREFERENCE) } }

    private val _language = mutableStateOf(Config.DEFAULT_LANGUAGE)
    val languageState: State<String> get() = _language
    var language: String
        get() = _language.value
        set(value) { _language.value = value; saveString(Config.PREF_LANGUAGE, value) }

    private val _dynamicColor = mutableStateOf(true)
    val dynamicColorState: State<Boolean> get() = _dynamicColor
    var dynamicColor: Boolean
        get() = _dynamicColor.value
        set(value) { if (_dynamicColor.value != value) { _dynamicColor.value = value; saveBoolean(Config.PREF_DYNAMIC_COLOR, value); notifyChanged(Config.PREF_DYNAMIC_COLOR) } }

    private val _accentColorName = mutableStateOf(Config.ACCENT_ELECTRIC_BLUE)
    val accentColorNameState: State<String> get() = _accentColorName
    var accentColorName: String
        get() = _accentColorName.value
        set(value) { if (_accentColorName.value != value) { _accentColorName.value = value; saveString(Config.PREF_ACCENT_COLOR_NAME, value); notifyChanged(Config.PREF_ACCENT_COLOR_NAME) } }

    private val _cardStyle = mutableStateOf(Config.CARD_STYLE_DETAILED)
    var cardStyle: String
        get() = _cardStyle.value
        set(value) { if (_cardStyle.value != value) { _cardStyle.value = value; saveString(Config.PREF_CARD_STYLE, value); notifyChanged(Config.PREF_CARD_STYLE) } }

    private val _showQualityBadge = mutableStateOf(true)
    var showQualityBadge: Boolean
        get() = _showQualityBadge.value
        set(value) { if (_showQualityBadge.value != value) { _showQualityBadge.value = value; saveBoolean(Config.PREF_SHOW_QUALITY_BADGE, value); notifyChanged(Config.PREF_SHOW_QUALITY_BADGE) } }

    private val _showRealtimeSpeedCard = mutableStateOf(false)
    var showRealtimeSpeedCard: Boolean
        get() = _showRealtimeSpeedCard.value
        set(value) { if (_showRealtimeSpeedCard.value != value) { _showRealtimeSpeedCard.value = value; saveBoolean(Config.PREF_SHOW_REALTIME_SPEED_CARD, value); notifyChanged(Config.PREF_SHOW_REALTIME_SPEED_CARD) } }

    private val _amoledMode = mutableStateOf(false)
    val amoledModeState: State<Boolean> get() = _amoledMode
    var amoledMode: Boolean
        get() = _amoledMode.value
        set(value) { if (_amoledMode.value != value) { _amoledMode.value = value; saveBoolean(Config.PREF_AMOLED_MODE, value); notifyChanged(Config.PREF_AMOLED_MODE) } }

    private val _markAsMV = mutableStateOf(false)
    val markAsMVState: State<Boolean> get() = _markAsMV
    var markAsMV: Boolean
        get() = _markAsMV.value
        set(value) { if (_markAsMV.value != value) { _markAsMV.value = value; saveBoolean(Config.PREF_MARK_AS_MV, value); notifyChanged(Config.PREF_MARK_AS_MV) } }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(Config.PREFS_NAME, Context.MODE_PRIVATE)

        _selectedQuality.value = prefs.getString(Config.PREF_SELECTED_QUALITY, Config.DEFAULT_QUALITY) ?: Config.DEFAULT_QUALITY
        _selectedVideoFormat.value = prefs.getString(Config.PREF_SELECTED_VIDEO_FORMAT, Config.FORMAT_MP4) ?: Config.FORMAT_MP4
        _selectedAudioFormat.value = prefs.getString(Config.PREF_SELECTED_AUDIO_FORMAT, Config.FORMAT_MP3) ?: Config.FORMAT_MP3
        _notificationsEnabled.value = prefs.getBoolean(Config.PREF_NOTIFICATIONS_ENABLED, true)
        _dataSaverEnabled.value = prefs.getBoolean(Config.PREF_DATA_SAVER_ENABLED, false)

        val savedLocation = prefs.getString(Config.PREF_DOWNLOAD_LOCATION, Config.PATH_DOWNLOAD_LOCATION_DEFAULT) ?: Config.PATH_DOWNLOAD_LOCATION_DEFAULT
        if (savedLocation == "Downloads/FabiDownloader" || savedLocation == "Download/FabiDownloader") {
            _downloadLocation.value = Config.PATH_DOWNLOAD_LOCATION_DEFAULT
            saveString(Config.PREF_DOWNLOAD_LOCATION, Config.PATH_DOWNLOAD_LOCATION_DEFAULT)
        } else {
            _downloadLocation.value = savedLocation
        }

        _maxSpeed.value = prefs.getString(Config.PREF_MAX_SPEED, Config.SPEED_UNLIMITED) ?: Config.SPEED_UNLIMITED
        _themePreference.value = prefs.getString(Config.PREF_THEME_PREFERENCE, Config.THEME_SYSTEM) ?: Config.THEME_SYSTEM
        _language.value = prefs.getString(Config.PREF_LANGUAGE, Config.DEFAULT_LANGUAGE) ?: Config.DEFAULT_LANGUAGE
        _confirmOnDelete.value = prefs.getBoolean(Config.PREF_CONFIRM_ON_DELETE, true)
        _concurrentFragments.value = prefs.getString(Config.PREF_CONCURRENT_FRAGMENTS, Config.DEFAULT_CONCURRENT_FRAGMENTS) ?: Config.DEFAULT_CONCURRENT_FRAGMENTS
        _embedSubtitles.value = prefs.getBoolean(Config.PREF_EMBED_SUBTITLES, false)
        _playlistEnabled.value = prefs.getBoolean(Config.PREF_PLAYLIST_ENABLED, false)
        _maxConcurrentDownloads.value = prefs.getInt(Config.PREF_MAX_CONCURRENT_DOWNLOADS, 2)
        _earlyStartThreshold.value = prefs.getInt(Config.PREF_EARLY_START_THRESHOLD, 0)
        _clipboardAction.value = prefs.getString(Config.PREF_CLIPBOARD_ACTION, Config.CLIPBOARD_ACTION_BANNER) ?: Config.CLIPBOARD_ACTION_BANNER
        _lastDownloadedOptionId.value = prefs.getString(Config.PREF_LAST_DOWNLOADED_OPTION_ID, "") ?: ""

        _customArguments.value = prefs.getString(Config.PREF_CUSTOM_ARGUMENTS, "") ?: ""
        _cookies.value = prefs.getString(Config.PREF_COOKIES, "") ?: ""
        syncCookiesFile(context, _cookies.value)
        _customUserAgent.value = prefs.getString(Config.PREF_CUSTOM_USER_AGENT, "") ?: ""
        _sponsorBlockEnabled.value = prefs.getBoolean(Config.PREF_SPONSOR_BLOCK_ENABLED, false)
        _embedThumbnail.value = prefs.getBoolean(Config.PREF_EMBED_THUMBNAIL, true)
        _embedMetadata.value = prefs.getBoolean(Config.PREF_EMBED_METADATA, true)
        _bypassGeo.value = prefs.getBoolean(Config.PREF_BYPASS_GEO, true)
        _bypassSslVerification.value = prefs.getBoolean(Config.PREF_BYPASS_SSL_VERIFICATION, false)

        _showDownloadSpeedInNotification.value = prefs.getBoolean(Config.PREF_SHOW_DOWNLOAD_SPEED_IN_NOTIFICATION, true)
        _selectedPausedNotificationTimeout.value = prefs.getString(Config.PREF_SELECTED_PAUSED_NOTIFICATION_TIMEOUT, Config.TIMEOUT_10_MIN) ?: Config.TIMEOUT_10_MIN
        _batteryOptimizationEnabled.value = prefs.getBoolean(Config.PREF_BATTERY_OPTIMIZATION_ENABLED, true)
        _selectedBatteryLowThreshold.value = prefs.getString(Config.PREF_SELECTED_BATTERY_LOW_THRESHOLD, Config.BATTERY_THRESHOLD_20) ?: Config.BATTERY_THRESHOLD_20
        val rawLowAction = prefs.getString(Config.PREF_SELECTED_BATTERY_LOW_ACTION, Config.BATTERY_ACTION_OPTIMIZE) ?: Config.BATTERY_ACTION_OPTIMIZE
        _selectedBatteryLowAction.value = if (rawLowAction == "Suspender todo") Config.BATTERY_ACTION_OPTIMIZE else rawLowAction
        _keepHistory.value = prefs.getBoolean(Config.PREF_KEEP_HISTORY, true)
        _autoRetry.value = prefs.getBoolean(Config.PREF_AUTO_RETRY, false)
        _dynamicColor.value = prefs.getBoolean(Config.PREF_DYNAMIC_COLOR, true)
        _accentColorName.value = prefs.getString(Config.PREF_ACCENT_COLOR_NAME, Config.ACCENT_ELECTRIC_BLUE) ?: Config.ACCENT_ELECTRIC_BLUE
        val savedMargin = prefs.getString(Config.PREF_SELECTED_STORAGE_MARGIN, Config.STORAGE_MARGIN_500MB) ?: Config.STORAGE_MARGIN_500MB
        _selectedStorageMargin.value = if (savedMargin in storageMarginOptions) savedMargin else Config.STORAGE_MARGIN_500MB
        _cardStyle.value = prefs.getString(Config.PREF_CARD_STYLE, Config.CARD_STYLE_DETAILED) ?: Config.CARD_STYLE_DETAILED
        _showQualityBadge.value = prefs.getBoolean(Config.PREF_SHOW_QUALITY_BADGE, true)
        _showRealtimeSpeedCard.value = prefs.getBoolean(Config.PREF_SHOW_REALTIME_SPEED_CARD, false)
        _defaultAudioBitrate.value = prefs.getString(Config.PREF_DEFAULT_AUDIO_BITRATE, Config.BITRATE_192) ?: Config.BITRATE_192
        _notifyBatchComplete.value = prefs.getBoolean(Config.PREF_NOTIFY_BATCH_COMPLETE, true)
        _cleanTempOnCancel.value = prefs.getBoolean(Config.PREF_CLEAN_TEMP_ON_CANCEL, true)
        _quickShareMode.value = prefs.getBoolean(Config.PREF_QUICK_SHARE_MODE, true)
        _allowDuplicateDownloads.value = prefs.getBoolean(Config.PREF_ALLOW_DUPLICATE_DOWNLOADS, true)
        _embedChapters.value = prefs.getBoolean(Config.PREF_EMBED_CHAPTERS, true)
        _amoledMode.value = prefs.getBoolean(Config.PREF_AMOLED_MODE, false)
        _markAsMV.value = prefs.getBoolean(Config.PREF_MARK_AS_MV, false)
    }

    private fun saveString(key: String, value: String) {
        if (::prefs.isInitialized) prefs.edit { putString(key, value) }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        if (::prefs.isInitialized) prefs.edit { putBoolean(key, value) }
    }
}
