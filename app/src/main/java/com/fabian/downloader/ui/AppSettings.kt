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

object AppSettings {
    private lateinit var prefs: SharedPreferences

    val qualityOptions = listOf("360p", "480p", "720p", "1080p")
    val videoFormats = listOf("MP4", "WEBM")
    val audioFormats = listOf("MP3", "M4A", "OGG")
    val themeOptions = listOf("Sistema", "Claro", "Oscuro")
    val speedOptions = listOf("500 KB/s", "1 MB/s", "5 MB/s", "10 MB/s", "Ilimitada")

    private val _selectedQuality = mutableStateOf("720p")
    var selectedQuality: String
        get() = _selectedQuality.value
        set(value) {
            _selectedQuality.value = value
            saveString("selectedQuality", value)
        }

    private val _selectedVideoFormat = mutableStateOf("MP4")
    var selectedVideoFormat: String
        get() = _selectedVideoFormat.value
        set(value) {
            _selectedVideoFormat.value = value
            saveString("selectedVideoFormat", value)
        }

    private val _selectedAudioFormat = mutableStateOf("MP3")
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

    private val _downloadLocation = mutableStateOf("Downloads/FabiDownloader")
    var downloadLocation: String
        get() = _downloadLocation.value
        set(value) {
            _downloadLocation.value = value
            saveString("downloadLocation", value)
        }

    private val _maxSpeed = mutableStateOf("Ilimitada")
    var maxSpeed: String
        get() = _maxSpeed.value
        set(value) {
            _maxSpeed.value = value
            saveString("maxSpeed", value)
        }

    private val _themePreference = mutableStateOf("Sistema")
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

    private val _concurrentFragments = mutableStateOf("5")
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

    private val _cookiesText = mutableStateOf("")
    var cookiesText: String
        get() = _cookiesText.value
        set(value) {
            _cookiesText.value = value
            saveString("cookiesText", value)
            writeCookiesFile(value)
        }

    private val _customArguments = mutableStateOf("")
    var customArguments: String
        get() = _customArguments.value
        set(value) {
            _customArguments.value = value
            saveString("customArguments", value)
        }

    private val _proxyUrl = mutableStateOf("")
    var proxyUrl: String
        get() = _proxyUrl.value
        set(value) {
            _proxyUrl.value = value
            saveString("proxyUrl", value)
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

    private fun writeCookiesFile(text: String) {
        // Run I/O in a background thread to avoid ANR
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val context = com.fabian.downloader.MyApplication.getInstance()
                val file = java.io.File(context.filesDir, "cookies.txt")
                if (text.isBlank()) {
                    if (file.exists()) {
                        file.delete()
                    }
                } else {
                    file.writeText(text)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppSettings", "Error writing cookies file", e)
            }
        }
    }

    fun init(context: Context) {
        prefs = context.getSharedPreferences("fabi_downloader_prefs", Context.MODE_PRIVATE)
        
        _selectedQuality.value = prefs.getString("selectedQuality", "720p") ?: "720p"
        _selectedVideoFormat.value = prefs.getString("selectedVideoFormat", "MP4") ?: "MP4"
        _selectedAudioFormat.value = prefs.getString("selectedAudioFormat", "MP3") ?: "MP3"
        _notificationsEnabled.value = prefs.getBoolean("notificationsEnabled", true)
        _dataSaverEnabled.value = prefs.getBoolean("dataSaverEnabled", false)
        _downloadLocation.value = prefs.getString("downloadLocation", "Downloads/FabiDownloader") ?: "Downloads/FabiDownloader"
        _maxSpeed.value = prefs.getString("maxSpeed", "Ilimitada") ?: "Ilimitada"
        _themePreference.value = prefs.getString("themePreference", "Sistema") ?: "Sistema"
        _confirmOnDelete.value = prefs.getBoolean("confirmOnDelete", true)
        _concurrentFragments.value = prefs.getString("concurrentFragments", "5") ?: "5"
        _embedSubtitles.value = prefs.getBoolean("embedSubtitles", false)
        _playlistEnabled.value = prefs.getBoolean("playlistEnabled", false)
        _maxConcurrentDownloads.value = prefs.getInt("maxConcurrentDownloads", 2)
        _clipboardAction.value = prefs.getString("clipboardAction", "banner") ?: "banner"
        
        _cookiesText.value = prefs.getString("cookiesText", "") ?: ""
        writeCookiesFile(_cookiesText.value)

        _customArguments.value = prefs.getString("customArguments", "") ?: ""
        _proxyUrl.value = prefs.getString("proxyUrl", "") ?: ""
        _customUserAgent.value = prefs.getString("customUserAgent", "") ?: ""
        _sponsorBlockEnabled.value = prefs.getBoolean("sponsorBlockEnabled", false)
        _embedThumbnail.value = prefs.getBoolean("embedThumbnail", true)
        _embedMetadata.value = prefs.getBoolean("embedMetadata", true)
        _bypassGeo.value = prefs.getBoolean("bypassGeo", true)
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
