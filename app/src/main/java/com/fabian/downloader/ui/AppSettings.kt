package com.fabian.downloader.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fabian.downloader.configs.Config

object AppSettings {
    private lateinit var prefs: SharedPreferences

    private val listeners = mutableListOf<(String) -> Unit>()

    fun addListener(listener: (String) -> Unit) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: (String) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    private fun notifyChanged(key: String) {
        val targets = synchronized(listeners) { listeners.toList() }
        targets.forEach { listener ->
            try {
                listener(key)
            } catch (e: Exception) {
                android.util.Log.e("AppSettings", "Error en listener para clave $key", e)
            }
        }
    }

    val qualityOptions = listOf("Mejor disponible", "4K (2160p)", "1080p Full HD", "720p HD", "480p SD", "360p", "Solo audio (MP3)")
    val videoFormats = listOf(Config.FORMAT_MP4, Config.FORMAT_MKV, Config.FORMAT_WEBM)
    val audioFormats = listOf(Config.FORMAT_MP3, Config.FORMAT_M4A, Config.FORMAT_OGG, Config.FORMAT_WAV)
    val themeOptions = listOf("Sistema", "Claro", "Oscuro")
    val speedOptions = listOf(
        Config.SPEED_UNLIMITED,
        Config.SPEED_100K,
        Config.SPEED_250K,
        Config.SPEED_500K,
        Config.SPEED_1M,
        Config.SPEED_2M,
        Config.SPEED_5M,
        Config.SPEED_10M,
        Config.SPEED_20M,
        Config.SPEED_50M
    )
    val cardStyleOptions = listOf("Detallado con miniatura", "Compacto en lista", "Minimalista")
    val defaultAudioBitrateOptions = listOf("320 kbps (Máxima)", "256 kbps", "192 kbps (Estándar)", "128 kbps (Ligero)")

    private val _selectedQuality = mutableStateOf("720p")
    var selectedQuality: String
        get() = _selectedQuality.value
        set(value) {
            if (_selectedQuality.value != value) {
                _selectedQuality.value = value
                saveString("selectedQuality", value)
                notifyChanged("selectedQuality")
            }
        }

    private val _selectedVideoFormat = mutableStateOf(Config.FORMAT_MP4)
    var selectedVideoFormat: String
        get() = _selectedVideoFormat.value
        set(value) {
            if (_selectedVideoFormat.value != value) {
                _selectedVideoFormat.value = value
                saveString("selectedVideoFormat", value)
                notifyChanged("selectedVideoFormat")
            }
        }

    private val _selectedAudioFormat = mutableStateOf(Config.FORMAT_MP3)
    var selectedAudioFormat: String
        get() = _selectedAudioFormat.value
        set(value) {
            if (_selectedAudioFormat.value != value) {
                _selectedAudioFormat.value = value
                saveString("selectedAudioFormat", value)
                notifyChanged("selectedAudioFormat")
            }
        }

    private val _notificationsEnabled = mutableStateOf(true)
    var notificationsEnabled: Boolean
        get() = _notificationsEnabled.value
        set(value) {
            if (_notificationsEnabled.value != value) {
                _notificationsEnabled.value = value
                saveBoolean("notificationsEnabled", value)
                notifyChanged("notificationsEnabled")
            }
        }

    private val _dataSaverEnabled = mutableStateOf(false)
    var dataSaverEnabled: Boolean
        get() = _dataSaverEnabled.value
        set(value) {
            if (_dataSaverEnabled.value != value) {
                _dataSaverEnabled.value = value
                saveBoolean("dataSaverEnabled", value)
                notifyChanged("dataSaverEnabled")
            }
        }

    private val _downloadLocation = mutableStateOf(Config.PATH_DOWNLOAD_LOCATION_DEFAULT)
    var downloadLocation: String
        get() = _downloadLocation.value
        set(value) {
            if (_downloadLocation.value != value) {
                _downloadLocation.value = value
                com.fabian.downloader.utils.PathUtils.clearFolderCache()
                saveString("downloadLocation", value)
                notifyChanged("downloadLocation")
            }
        }

    private val _maxSpeed = mutableStateOf(Config.SPEED_UNLIMITED)
    var maxSpeed: String
        get() = _maxSpeed.value
        set(value) {
            if (_maxSpeed.value != value) {
                _maxSpeed.value = value
                saveString("maxSpeed", value)
                notifyChanged("maxSpeed")
            }
        }

    private val _themePreference = mutableStateOf("Sistema")
    val themePreferenceState: androidx.compose.runtime.State<String> get() = _themePreference
    
    var themePreference: String
        get() = _themePreference.value
        set(value) {
            if (_themePreference.value != value) {
                _themePreference.value = value
                saveString("themePreference", value)
                notifyChanged("themePreference")
            }
        }

    private val _language = mutableStateOf("Sistema")
    val languageState: androidx.compose.runtime.State<String> get() = _language
    var language: String
        get() = _language.value
        set(value) {
            _language.value = value
            saveString("language", value)
        }

    private val _confirmOnDelete = mutableStateOf(true)
    var confirmOnDelete: Boolean
        get() = _confirmOnDelete.value
        set(value) {
            _confirmOnDelete.value = value
            saveBoolean("confirmOnDelete", value)
        }

    private val _concurrentFragments = mutableStateOf("4")
    var concurrentFragments: String
        get() = _concurrentFragments.value
        set(value) {
            if (_concurrentFragments.value != value) {
                _concurrentFragments.value = value
                saveString("concurrentFragments", value)
                notifyChanged("concurrentFragments")
            }
        }

    private val _embedSubtitles = mutableStateOf(false)
    var embedSubtitles: Boolean
        get() = _embedSubtitles.value
        set(value) {
            if (_embedSubtitles.value != value) {
                _embedSubtitles.value = value
                saveBoolean("embedSubtitles", value)
                notifyChanged("embedSubtitles")
            }
        }

    private val _playlistEnabled = mutableStateOf(false)
    var playlistEnabled: Boolean
        get() = _playlistEnabled.value
        set(value) {
            _playlistEnabled.value = value
            saveBoolean("playlistEnabled", value)
        }

    private val _maxConcurrentDownloads = mutableStateOf(2)
    var maxConcurrentDownloads: Int
        get() = _maxConcurrentDownloads.value
        set(value) {
            if (_maxConcurrentDownloads.value != value) {
                _maxConcurrentDownloads.value = value
                if (::prefs.isInitialized) {
                    prefs.edit { putInt("maxConcurrentDownloads", value) }
                }
                notifyChanged("maxConcurrentDownloads")
            }
        }

    private val _earlyStartThreshold = mutableStateOf(0) // 0 means Desactivado, otherwise 95..99
    var earlyStartThreshold: Int
        get() = _earlyStartThreshold.value
        set(value) {
            if (_earlyStartThreshold.value != value) {
                _earlyStartThreshold.value = value
                if (::prefs.isInitialized) {
                    prefs.edit { putInt("earlyStartThreshold", value) }
                }
                notifyChanged("earlyStartThreshold")
            }
        }

    private val _clipboardAction = mutableStateOf("banner") // "banner", "auto", "disabled"
    var clipboardAction: String
        get() = _clipboardAction.value
        set(value) {
            _clipboardAction.value = value
            saveString("clipboardAction", value)
        }

    private val _lastDownloadedOptionId = mutableStateOf("")
    var lastDownloadedOptionId: String
        get() = _lastDownloadedOptionId.value
        set(value) {
            _lastDownloadedOptionId.value = value
            saveString("lastDownloadedOptionId", value)
        }

    private val _customArguments = mutableStateOf("")
    var customArguments: String
        get() = _customArguments.value
        set(value) {
            if (_customArguments.value != value) {
                _customArguments.value = value
                saveString("customArguments", value)
                notifyChanged("customArguments")
            }
        }

    private val _cookies = mutableStateOf("")
    var cookies: String
        get() = _cookies.value
        set(value) {
            if (_cookies.value != value) {
                _cookies.value = value
                saveString("cookies", value)
                syncCookiesFile(com.fabian.downloader.MyApplication.getInstance(), value)
                notifyChanged("cookies")
            }
        }

    private fun syncCookiesFile(context: Context, cookieContent: String) {
        try {
            val cookiesFile = java.io.File(context.filesDir, Config.COOKIES_FILE_NAME)
            if (cookieContent.trim().isEmpty()) {
                if (cookiesFile.exists()) {
                    cookiesFile.delete()
                }
            } else {
                cookiesFile.writeText(cookieContent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppSettings", "Error syncing cookies.txt: ${e.message}", e)
        }
    }

    private val _customUserAgent = mutableStateOf("")
    var customUserAgent: String
        get() = _customUserAgent.value
        set(value) {
            if (_customUserAgent.value != value) {
                _customUserAgent.value = value
                saveString("customUserAgent", value)
                notifyChanged("customUserAgent")
            }
        }

    private val _sponsorBlockEnabled = mutableStateOf(false)
    var sponsorBlockEnabled: Boolean
        get() = _sponsorBlockEnabled.value
        set(value) {
            if (_sponsorBlockEnabled.value != value) {
                _sponsorBlockEnabled.value = value
                saveBoolean("sponsorBlockEnabled", value)
                notifyChanged("sponsorBlockEnabled")
            }
        }

    private val _embedThumbnail = mutableStateOf(true)
    var embedThumbnail: Boolean
        get() = _embedThumbnail.value
        set(value) {
            if (_embedThumbnail.value != value) {
                _embedThumbnail.value = value
                saveBoolean("embedThumbnail", value)
                notifyChanged("embedThumbnail")
            }
        }

    private val _embedMetadata = mutableStateOf(true)
    var embedMetadata: Boolean
        get() = _embedMetadata.value
        set(value) {
            if (_embedMetadata.value != value) {
                _embedMetadata.value = value
                saveBoolean("embedMetadata", value)
                notifyChanged("embedMetadata")
            }
        }

    private val _bypassGeo = mutableStateOf(true)
    var bypassGeo: Boolean
        get() = _bypassGeo.value
        set(value) {
            if (_bypassGeo.value != value) {
                _bypassGeo.value = value
                saveBoolean("bypassGeo", value)
                notifyChanged("bypassGeo")
            }
        }

    private val _bypassSslVerification = mutableStateOf(false)
    var bypassSslVerification: Boolean
        get() = _bypassSslVerification.value
        set(value) {
            if (_bypassSslVerification.value != value) {
                _bypassSslVerification.value = value
                saveBoolean("bypassSslVerification", value)
                notifyChanged("bypassSslVerification")
            }
        }

    private val _showDownloadSpeedInNotification = mutableStateOf(true)
    var showDownloadSpeedInNotification: Boolean
        get() = _showDownloadSpeedInNotification.value
        set(value) {
            _showDownloadSpeedInNotification.value = value
            saveBoolean("showDownloadSpeedInNotification", value)
        }

    val pausedNotificationTimeoutOptions = listOf("1 minuto", "5 minutos", "10 minutos", "30 minutos", "Nunca")
    private val _selectedPausedNotificationTimeout = mutableStateOf("10 minutos")
    val selectedPausedNotificationTimeoutState: androidx.compose.runtime.State<String> get() = _selectedPausedNotificationTimeout
    var selectedPausedNotificationTimeout: String
        get() = _selectedPausedNotificationTimeout.value
        set(value) {
            _selectedPausedNotificationTimeout.value = value
            saveString("selectedPausedNotificationTimeout", value)
            notifyChanged("selectedPausedNotificationTimeout")
        }

    val pausedNotificationTimeoutMs: Long
        get() = when (selectedPausedNotificationTimeout) {
            "1 minuto" -> 60L * 1000L
            "5 minutos" -> 5L * 60L * 1000L
            "10 minutos" -> 10L * 60L * 1000L
            "30 minutos" -> 30L * 60L * 1000L
            else -> 0L // Nunca
        }

    private val _batteryOptimizationEnabled = mutableStateOf(true)
    var batteryOptimizationEnabled: Boolean
        get() = _batteryOptimizationEnabled.value
        set(value) {
            _batteryOptimizationEnabled.value = value
            saveBoolean("batteryOptimizationEnabled", value)
            notifyChanged("batteryOptimizationEnabled")
        }

    val batteryLowThresholdOptions = listOf("15%", "20%", "25%", "30%")
    private val _selectedBatteryLowThreshold = mutableStateOf("20%")
    val selectedBatteryLowThresholdState: androidx.compose.runtime.State<String> get() = _selectedBatteryLowThreshold
    var selectedBatteryLowThreshold: String
        get() = _selectedBatteryLowThreshold.value
        set(value) {
            _selectedBatteryLowThreshold.value = value
            saveString("selectedBatteryLowThreshold", value)
            notifyChanged("selectedBatteryLowThreshold")
        }

    val batteryLowThresholdInt: Int
        get() = selectedBatteryLowThreshold.replace("%", "").toIntOrNull() ?: 20

    val batteryLowActionOptions = listOf("Optimizar recursos", "Limitar concurrencia")
    private val _selectedBatteryLowAction = mutableStateOf("Optimizar recursos")
    val selectedBatteryLowActionState: androidx.compose.runtime.State<String> get() = _selectedBatteryLowAction
    var selectedBatteryLowAction: String
        get() = _selectedBatteryLowAction.value
        set(value) {
            _selectedBatteryLowAction.value = value
            saveString("selectedBatteryLowAction", value)
            notifyChanged("selectedBatteryLowAction")
        }

    val batteryLowAction: String
        get() = selectedBatteryLowAction

    private val _keepHistory = mutableStateOf(true)
    var keepHistory: Boolean
        get() = _keepHistory.value
        set(value) {
            _keepHistory.value = value
            saveBoolean("keepHistory", value)
        }

    private val _autoRetry = mutableStateOf(false)
    var autoRetry: Boolean
        get() = _autoRetry.value
        set(value) {
            if (_autoRetry.value != value) {
                _autoRetry.value = value
                saveBoolean("autoRetry", value)
                notifyChanged("autoRetry")
            }
        }

    private val _dynamicColor = mutableStateOf(true)
    val dynamicColorState: androidx.compose.runtime.State<Boolean> get() = _dynamicColor
    var dynamicColor: Boolean
        get() = _dynamicColor.value
        set(value) {
            if (_dynamicColor.value != value) {
                _dynamicColor.value = value
                saveBoolean("dynamicColor", value)
                notifyChanged("dynamicColor")
            }
        }

    private val _markAsMV = mutableStateOf(false)
    val markAsMVState: androidx.compose.runtime.State<Boolean> get() = _markAsMV
    var markAsMV: Boolean
        get() = _markAsMV.value
        set(value) {
            if (_markAsMV.value != value) {
                _markAsMV.value = value
                saveBoolean("markAsMV", value)
                notifyChanged("markAsMV")
            }
        }

    private val _accentColorName = mutableStateOf("Azul Eléctrico")
    val accentColorNameState: androidx.compose.runtime.State<String> get() = _accentColorName
    val accentColorOptions = listOf("Azul Eléctrico", "Verde Esmeralda", "Púrpura Real", "Naranja Sunset", "Rosa Hot", "Gris Acero")
    var accentColorName: String
        get() = _accentColorName.value
        set(value) {
            if (_accentColorName.value != value) {
                _accentColorName.value = value
                saveString("accentColorName", value)
                notifyChanged("accentColorName")
            }
        }

    val storageMarginOptions = listOf("Desactivado", "100 MB", "250 MB", "500 MB", "1 GB", "2 GB", "3 GB", "5 GB", "10 GB")
    private val _selectedStorageMargin = mutableStateOf("500 MB")
    val selectedStorageMarginState: androidx.compose.runtime.State<String> get() = _selectedStorageMargin
    var selectedStorageMargin: String
        get() = _selectedStorageMargin.value
        set(value) {
            _selectedStorageMargin.value = value
            saveString("selectedStorageMargin", value)
            notifyChanged("selectedStorageMargin")
        }

    val storageMarginBytes: Long
        get() = when (selectedStorageMargin) {
            "100 MB" -> 100L * 1024L * 1024L
            "250 MB" -> 250L * 1024L * 1024L
            "500 MB" -> 500L * 1024L * 1024L
            "1 GB" -> 1024L * 1024L * 1024L
            "2 GB" -> 2L * 1024L * 1024L * 1024L
            "3 GB" -> 3L * 1024L * 1024L * 1024L
            "5 GB" -> 5L * 1024L * 1024L * 1024L
            "10 GB" -> 10L * 1024L * 1024L * 1024L
            else -> 0L // Desactivado
        }

    private val _cardStyle = mutableStateOf("Detallado con miniatura")
    var cardStyle: String
        get() = _cardStyle.value
        set(value) {
            if (_cardStyle.value != value) {
                _cardStyle.value = value
                saveString("cardStyle", value)
                notifyChanged("cardStyle")
            }
        }

    private val _showQualityBadge = mutableStateOf(true)
    var showQualityBadge: Boolean
        get() = _showQualityBadge.value
        set(value) {
            if (_showQualityBadge.value != value) {
                _showQualityBadge.value = value
                saveBoolean("showQualityBadge", value)
                notifyChanged("showQualityBadge")
            }
        }

    private val _showRealtimeSpeedCard = mutableStateOf(false)
    var showRealtimeSpeedCard: Boolean
        get() = _showRealtimeSpeedCard.value
        set(value) {
            if (_showRealtimeSpeedCard.value != value) {
                _showRealtimeSpeedCard.value = value
                saveBoolean("showRealtimeSpeedCard", value)
                notifyChanged("showRealtimeSpeedCard")
            }
        }

    private val _defaultAudioBitrate = mutableStateOf("192 kbps (Estándar)")
    var defaultAudioBitrate: String
        get() = _defaultAudioBitrate.value
        set(value) {
            if (_defaultAudioBitrate.value != value) {
                _defaultAudioBitrate.value = value
                saveString("defaultAudioBitrate", value)
                notifyChanged("defaultAudioBitrate")
            }
        }

    private val _notifyBatchComplete = mutableStateOf(true)
    var notifyBatchComplete: Boolean
        get() = _notifyBatchComplete.value
        set(value) {
            if (_notifyBatchComplete.value != value) {
                _notifyBatchComplete.value = value
                saveBoolean("notifyBatchComplete", value)
                notifyChanged("notifyBatchComplete")
            }
        }

    private val _cleanTempOnCancel = mutableStateOf(true)
    var cleanTempOnCancel: Boolean
        get() = _cleanTempOnCancel.value
        set(value) {
            if (_cleanTempOnCancel.value != value) {
                _cleanTempOnCancel.value = value
                saveBoolean("cleanTempOnCancel", value)
                notifyChanged("cleanTempOnCancel")
            }
        }

    private val _quickShareMode = mutableStateOf(true)
    val quickShareModeState: androidx.compose.runtime.State<Boolean> get() = _quickShareMode
    var quickShareMode: Boolean
        get() = _quickShareMode.value
        set(value) {
            if (_quickShareMode.value != value) {
                _quickShareMode.value = value
                saveBoolean("quickShareMode", value)
                notifyChanged("quickShareMode")
            }
        }

    private val _allowDuplicateDownloads = mutableStateOf(true)
    var allowDuplicateDownloads: Boolean
        get() = _allowDuplicateDownloads.value
        set(value) {
            if (_allowDuplicateDownloads.value != value) {
                _allowDuplicateDownloads.value = value
                saveBoolean("allowDuplicateDownloads", value)
                notifyChanged("allowDuplicateDownloads")
            }
        }

    private val _embedChapters = mutableStateOf(true)
    var embedChapters: Boolean
        get() = _embedChapters.value
        set(value) {
            if (_embedChapters.value != value) {
                _embedChapters.value = value
                saveBoolean("embedChapters", value)
                notifyChanged("embedChapters")
            }
        }

    private val _amoledMode = mutableStateOf(false)
    val amoledModeState: androidx.compose.runtime.State<Boolean> get() = _amoledMode
    var amoledMode: Boolean
        get() = _amoledMode.value
        set(value) {
            if (_amoledMode.value != value) {
                _amoledMode.value = value
                saveBoolean("amoledMode", value)
                notifyChanged("amoledMode")
            }
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("fabi_downloader_prefs", Context.MODE_PRIVATE)
        
        _selectedQuality.value = prefs.getString("selectedQuality", "720p") ?: "720p"
        _selectedVideoFormat.value = prefs.getString("selectedVideoFormat", Config.FORMAT_MP4) ?: Config.FORMAT_MP4
        _selectedAudioFormat.value = prefs.getString("selectedAudioFormat", Config.FORMAT_MP3) ?: Config.FORMAT_MP3
        _notificationsEnabled.value = prefs.getBoolean("notificationsEnabled", true)
        _dataSaverEnabled.value = prefs.getBoolean("dataSaverEnabled", false)
        val savedLocation = prefs.getString("downloadLocation", Config.PATH_DOWNLOAD_LOCATION_DEFAULT) ?: Config.PATH_DOWNLOAD_LOCATION_DEFAULT
        if (savedLocation == "Downloads/FabiDownloader" || savedLocation == "Download/FabiDownloader") {
            _downloadLocation.value = Config.PATH_DOWNLOAD_LOCATION_DEFAULT
            saveString("downloadLocation", Config.PATH_DOWNLOAD_LOCATION_DEFAULT)
        } else {
            _downloadLocation.value = savedLocation
        }
        _maxSpeed.value = prefs.getString("maxSpeed", Config.SPEED_UNLIMITED) ?: Config.SPEED_UNLIMITED
        _themePreference.value = prefs.getString("themePreference", "Sistema") ?: "Sistema"
        _language.value = prefs.getString("language", "Sistema") ?: "Sistema"
        _confirmOnDelete.value = prefs.getBoolean("confirmOnDelete", true)
        _concurrentFragments.value = prefs.getString("concurrentFragments", "4") ?: "4"
        _embedSubtitles.value = prefs.getBoolean("embedSubtitles", false)
        _playlistEnabled.value = prefs.getBoolean("playlistEnabled", false)
        _maxConcurrentDownloads.value = prefs.getInt("maxConcurrentDownloads", 2)
        _earlyStartThreshold.value = prefs.getInt("earlyStartThreshold", 0)
        _clipboardAction.value = prefs.getString("clipboardAction", "banner") ?: "banner"
        _lastDownloadedOptionId.value = prefs.getString("lastDownloadedOptionId", "") ?: ""
        
        _customArguments.value = prefs.getString("customArguments", "") ?: ""
        _cookies.value = prefs.getString("cookies", "") ?: ""
        syncCookiesFile(context, _cookies.value)
        _customUserAgent.value = prefs.getString("customUserAgent", "") ?: ""
        _sponsorBlockEnabled.value = prefs.getBoolean("sponsorBlockEnabled", false)
        _embedThumbnail.value = prefs.getBoolean("embedThumbnail", true)
        _embedMetadata.value = prefs.getBoolean("embedMetadata", true)
        _bypassGeo.value = prefs.getBoolean("bypassGeo", true)
        _bypassSslVerification.value = prefs.getBoolean("bypassSslVerification", false)

        _showDownloadSpeedInNotification.value = prefs.getBoolean("showDownloadSpeedInNotification", true)
        _selectedPausedNotificationTimeout.value = prefs.getString("selectedPausedNotificationTimeout", "10 minutos") ?: "10 minutos"
        _batteryOptimizationEnabled.value = prefs.getBoolean("batteryOptimizationEnabled", true)
        _selectedBatteryLowThreshold.value = prefs.getString("selectedBatteryLowThreshold", "20%") ?: "20%"
        val rawLowAction = prefs.getString("selectedBatteryLowAction", "Optimizar recursos") ?: "Optimizar recursos"
        _selectedBatteryLowAction.value = if (rawLowAction == "Suspender todo") "Optimizar recursos" else rawLowAction
        _keepHistory.value = prefs.getBoolean("keepHistory", true)
        _autoRetry.value = prefs.getBoolean("autoRetry", false)
        _dynamicColor.value = prefs.getBoolean("dynamicColor", true)
        _accentColorName.value = prefs.getString("accentColorName", "Azul Eléctrico") ?: "Azul Eléctrico"
        val savedMargin = prefs.getString("selectedStorageMargin", "500 MB") ?: "500 MB"
        _selectedStorageMargin.value = if (savedMargin in storageMarginOptions) savedMargin else "500 MB"
        _cardStyle.value = prefs.getString("cardStyle", "Detallado con miniatura") ?: "Detallado con miniatura"
        _showQualityBadge.value = prefs.getBoolean("showQualityBadge", true)
        _showRealtimeSpeedCard.value = prefs.getBoolean("showRealtimeSpeedCard", false)
        _defaultAudioBitrate.value = prefs.getString("defaultAudioBitrate", "192 kbps (Estándar)") ?: "192 kbps (Estándar)"
        _notifyBatchComplete.value = prefs.getBoolean("notifyBatchComplete", true)
        _cleanTempOnCancel.value = prefs.getBoolean("cleanTempOnCancel", true)
        _quickShareMode.value = prefs.getBoolean("quickShareMode", true)
        _allowDuplicateDownloads.value = prefs.getBoolean("allowDuplicateDownloads", true)
        _embedChapters.value = prefs.getBoolean("embedChapters", true)
        _amoledMode.value = prefs.getBoolean("amoledMode", false)
        _markAsMV.value = prefs.getBoolean("markAsMV", false)
    }

    private fun saveString(key: String, value: String) {
        if (::prefs.isInitialized) {
            prefs.edit { putString(key, value) }
        }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit { putBoolean(key, value) }
        }
    }
}
