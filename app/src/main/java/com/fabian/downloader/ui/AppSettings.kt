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
import com.fabian.downloader.utils.Config

object AppSettings {
    private lateinit var prefs: SharedPreferences

    val qualityOptions = listOf("Mejor disponible", "4K (2160p)", "1080p Full HD", "720p HD", "480p SD", "360p", "Solo audio (MP3)")
    val videoFormats = listOf(Config.FORMAT_MP4, Config.FORMAT_WEBM)
    val audioFormats = listOf(Config.FORMAT_MP3, Config.FORMAT_M4A, Config.FORMAT_OGG)
    val themeOptions = listOf("Sistema", "Claro", "Oscuro")
    val speedOptions = listOf(Config.SPEED_500K, Config.SPEED_1M, Config.SPEED_5M, Config.SPEED_10M, Config.SPEED_UNLIMITED)

    private val _selectedQuality = mutableStateOf("720p")
    var selectedQuality: String
        get() = _selectedQuality.value
        set(value) {
            _selectedQuality.value = value
            saveString("selectedQuality", value)
        }

    private val _selectedVideoFormat = mutableStateOf(Config.FORMAT_MP4)
    var selectedVideoFormat: String
        get() = _selectedVideoFormat.value
        set(value) {
            _selectedVideoFormat.value = value
            saveString("selectedVideoFormat", value)
        }

    private val _selectedAudioFormat = mutableStateOf(Config.FORMAT_MP3)
    var selectedAudioFormat: String
        get() = _selectedAudioFormat.value
        set(value) {
            _selectedAudioFormat.value = value
            saveString("selectedAudioFormat", value)
        }

    private val _notificationsEnabled = mutableStateOf(true)
    var notificationsEnabled: Boolean
        get() = _notificationsEnabled.value
        set(value) {
            _notificationsEnabled.value = value
            saveBoolean("notificationsEnabled", value)
        }

    private val _dataSaverEnabled = mutableStateOf(false)
    var dataSaverEnabled: Boolean
        get() = _dataSaverEnabled.value
        set(value) {
            _dataSaverEnabled.value = value
            saveBoolean("dataSaverEnabled", value)
        }

    private val _downloadLocation = mutableStateOf(Config.PATH_DOWNLOAD_LOCATION_DEFAULT)
    var downloadLocation: String
        get() = _downloadLocation.value
        set(value) {
            _downloadLocation.value = value
            saveString("downloadLocation", value)
        }

    private val _maxSpeed = mutableStateOf(Config.SPEED_UNLIMITED)
    var maxSpeed: String
        get() = _maxSpeed.value
        set(value) {
            _maxSpeed.value = value
            saveString("maxSpeed", value)
        }

    private val _themePreference = mutableStateOf("Sistema")
    val themePreferenceState: androidx.compose.runtime.State<String> get() = _themePreference
    
    var themePreference: String
        get() = _themePreference.value
        set(value) {
            _themePreference.value = value
            saveString("themePreference", value)
        }

    private val _confirmOnDelete = mutableStateOf(true)
    var confirmOnDelete: Boolean
        get() = _confirmOnDelete.value
        set(value) {
            _confirmOnDelete.value = value
            saveBoolean("confirmOnDelete", value)
        }

    private val _concurrentFragments = mutableStateOf("10")
    var concurrentFragments: String
        get() = _concurrentFragments.value
        set(value) {
            _concurrentFragments.value = value
            saveString("concurrentFragments", value)
        }

    private val _embedSubtitles = mutableStateOf(false)
    var embedSubtitles: Boolean
        get() = _embedSubtitles.value
        set(value) {
            _embedSubtitles.value = value
            saveBoolean("embedSubtitles", value)
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
            _maxConcurrentDownloads.value = value
            if (::prefs.isInitialized) {
                prefs.edit { putInt("maxConcurrentDownloads", value) }
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
            _customArguments.value = value
            saveString("customArguments", value)
        }

    private val _cookies = mutableStateOf("")
    var cookies: String
        get() = _cookies.value
        set(value) {
            _cookies.value = value
            saveString("cookies", value)
        }

    private val _customUserAgent = mutableStateOf("")
    var customUserAgent: String
        get() = _customUserAgent.value
        set(value) {
            _customUserAgent.value = value
            saveString("customUserAgent", value)
        }

    private val _sponsorBlockEnabled = mutableStateOf(false)
    var sponsorBlockEnabled: Boolean
        get() = _sponsorBlockEnabled.value
        set(value) {
            _sponsorBlockEnabled.value = value
            saveBoolean("sponsorBlockEnabled", value)
        }

    private val _embedThumbnail = mutableStateOf(true)
    var embedThumbnail: Boolean
        get() = _embedThumbnail.value
        set(value) {
            _embedThumbnail.value = value
            saveBoolean("embedThumbnail", value)
        }

    private val _embedMetadata = mutableStateOf(true)
    var embedMetadata: Boolean
        get() = _embedMetadata.value
        set(value) {
            _embedMetadata.value = value
            saveBoolean("embedMetadata", value)
        }

    private val _bypassGeo = mutableStateOf(true)
    var bypassGeo: Boolean
        get() = _bypassGeo.value
        set(value) {
            _bypassGeo.value = value
            saveBoolean("bypassGeo", value)
        }

    private val _showDownloadSpeedInNotification = mutableStateOf(true)
    var showDownloadSpeedInNotification: Boolean
        get() = _showDownloadSpeedInNotification.value
        set(value) {
            _showDownloadSpeedInNotification.value = value
            saveBoolean("showDownloadSpeedInNotification", value)
        }

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
            _autoRetry.value = value
            saveBoolean("autoRetry", value)
        }

    private val _dynamicColor = mutableStateOf(true)
    val dynamicColorState: androidx.compose.runtime.State<Boolean> get() = _dynamicColor
    var dynamicColor: Boolean
        get() = _dynamicColor.value
        set(value) {
            _dynamicColor.value = value
            saveBoolean("dynamicColor", value)
        }

    private val _accentColorName = mutableStateOf("Azul Eléctrico")
    val accentColorNameState: androidx.compose.runtime.State<String> get() = _accentColorName
    val accentColorOptions = listOf("Azul Eléctrico", "Verde Esmeralda", "Púrpura Real", "Naranja Sunset", "Rosa Hot", "Gris Acero")
    var accentColorName: String
        get() = _accentColorName.value
        set(value) {
            _accentColorName.value = value
            saveString("accentColorName", value)
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("fabi_downloader_prefs", Context.MODE_PRIVATE)
        
        _selectedQuality.value = prefs.getString("selectedQuality", "720p") ?: "720p"
        _selectedVideoFormat.value = prefs.getString("selectedVideoFormat", Config.FORMAT_MP4) ?: Config.FORMAT_MP4
        _selectedAudioFormat.value = prefs.getString("selectedAudioFormat", Config.FORMAT_MP3) ?: Config.FORMAT_MP3
        _notificationsEnabled.value = prefs.getBoolean("notificationsEnabled", true)
        _dataSaverEnabled.value = prefs.getBoolean("dataSaverEnabled", false)
        _downloadLocation.value = prefs.getString("downloadLocation", Config.PATH_DOWNLOAD_LOCATION_DEFAULT) ?: Config.PATH_DOWNLOAD_LOCATION_DEFAULT
        _maxSpeed.value = prefs.getString("maxSpeed", Config.SPEED_UNLIMITED) ?: Config.SPEED_UNLIMITED
        _themePreference.value = prefs.getString("themePreference", "Sistema") ?: "Sistema"
        _confirmOnDelete.value = prefs.getBoolean("confirmOnDelete", true)
        _concurrentFragments.value = prefs.getString("concurrentFragments", "10") ?: "10"
        _embedSubtitles.value = prefs.getBoolean("embedSubtitles", false)
        _playlistEnabled.value = prefs.getBoolean("playlistEnabled", false)
        _maxConcurrentDownloads.value = prefs.getInt("maxConcurrentDownloads", 2)
        _clipboardAction.value = prefs.getString("clipboardAction", "banner") ?: "banner"
        _lastDownloadedOptionId.value = prefs.getString("lastDownloadedOptionId", "") ?: ""
        
        _customArguments.value = prefs.getString("customArguments", "") ?: ""
        _cookies.value = prefs.getString("cookies", "") ?: ""
        _customUserAgent.value = prefs.getString("customUserAgent", "") ?: ""
        _sponsorBlockEnabled.value = prefs.getBoolean("sponsorBlockEnabled", false)
        _embedThumbnail.value = prefs.getBoolean("embedThumbnail", true)
        _embedMetadata.value = prefs.getBoolean("embedMetadata", true)
        _bypassGeo.value = prefs.getBoolean("bypassGeo", true)

        _showDownloadSpeedInNotification.value = prefs.getBoolean("showDownloadSpeedInNotification", true)
        _keepHistory.value = prefs.getBoolean("keepHistory", true)
        _autoRetry.value = prefs.getBoolean("autoRetry", false)
        _dynamicColor.value = prefs.getBoolean("dynamicColor", true)
        _accentColorName.value = prefs.getString("accentColorName", "Azul Eléctrico") ?: "Azul Eléctrico"
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
