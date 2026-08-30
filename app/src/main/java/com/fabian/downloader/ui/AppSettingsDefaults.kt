package com.fabian.downloader.ui

import com.fabian.downloader.configs.Config

object AppSettingsDefaults {
    val qualityOptions = listOf(
        Config.QUALITY_BEST,
        Config.QUALITY_4K,
        Config.QUALITY_1080P,
        Config.QUALITY_720P,
        Config.QUALITY_480P,
        Config.QUALITY_360P,
        Config.QUALITY_AUDIO_ONLY
    )

    val videoFormats = listOf(Config.FORMAT_MP4, Config.FORMAT_MKV, Config.FORMAT_WEBM)
    val audioFormats = listOf(Config.FORMAT_MP3, Config.FORMAT_M4A, Config.FORMAT_OGG, Config.FORMAT_WAV)
    val themeOptions = listOf(Config.THEME_SYSTEM, Config.THEME_LIGHT, Config.THEME_DARK)

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

    val cardStyleOptions = listOf(
        Config.CARD_STYLE_DETAILED,
        Config.CARD_STYLE_COMPACT,
        Config.CARD_STYLE_MINIMAL
    )

    val defaultAudioBitrateOptions = listOf(
        Config.BITRATE_320,
        Config.BITRATE_256,
        Config.BITRATE_192,
        Config.BITRATE_128
    )

    val pausedNotificationTimeoutOptions = listOf(
        Config.TIMEOUT_1_MIN,
        Config.TIMEOUT_5_MIN,
        Config.TIMEOUT_10_MIN,
        Config.TIMEOUT_30_MIN,
        Config.TIMEOUT_NEVER
    )

    val batteryLowThresholdOptions = listOf(
        Config.BATTERY_THRESHOLD_15,
        Config.BATTERY_THRESHOLD_20,
        Config.BATTERY_THRESHOLD_25,
        Config.BATTERY_THRESHOLD_30
    )

    val batteryLowActionOptions = listOf(
        Config.BATTERY_ACTION_OPTIMIZE,
        Config.BATTERY_ACTION_LIMIT
    )

    val accentColorOptions = listOf(
        Config.ACCENT_ELECTRIC_BLUE,
        Config.ACCENT_EMERALD_GREEN,
        Config.ACCENT_ROYAL_PURPLE,
        Config.ACCENT_SUNSET_ORANGE,
        Config.ACCENT_HOT_PINK,
        Config.ACCENT_STEEL_GRAY
    )

    val storageMarginOptions = listOf(
        Config.STORAGE_MARGIN_DISABLED,
        Config.STORAGE_MARGIN_100MB,
        Config.STORAGE_MARGIN_250MB,
        Config.STORAGE_MARGIN_500MB,
        Config.STORAGE_MARGIN_1GB,
        Config.STORAGE_MARGIN_2GB,
        Config.STORAGE_MARGIN_3GB,
        Config.STORAGE_MARGIN_5GB,
        Config.STORAGE_MARGIN_10GB
    )

    fun calculatePausedTimeoutMs(selected: String): Long = when (selected) {
        Config.TIMEOUT_1_MIN -> 60L * 1000L
        Config.TIMEOUT_5_MIN -> 5L * 60L * 1000L
        Config.TIMEOUT_10_MIN -> 10L * 60L * 1000L
        Config.TIMEOUT_30_MIN -> 30L * 60L * 1000L
        else -> 0L
    }

    fun calculateStorageMarginBytes(selected: String): Long = when (selected) {
        Config.STORAGE_MARGIN_100MB -> 100L * 1024L * 1024L
        Config.STORAGE_MARGIN_250MB -> 250L * 1024L * 1024L
        Config.STORAGE_MARGIN_500MB -> 500L * 1024L * 1024L
        Config.STORAGE_MARGIN_1GB -> 1024L * 1024L * 1024L
        Config.STORAGE_MARGIN_2GB -> 2L * 1024L * 1024L * 1024L
        Config.STORAGE_MARGIN_3GB -> 3L * 1024L * 1024L * 1024L
        Config.STORAGE_MARGIN_5GB -> 5L * 1024L * 1024L * 1024L
        Config.STORAGE_MARGIN_10GB -> 10L * 1024L * 1024L * 1024L
        else -> 0L
    }
}
