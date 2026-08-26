package com.fabian.downloader.utils

import android.content.Context
import com.fabian.downloader.R
import com.fabian.downloader.configs.Config

object SettingsLabels {

    fun getQualityLabel(context: Context, quality: String): String = when (quality.lowercase()) {
        Config.QUALITY_BEST -> context.getString(R.string.settings_quality_best)
        Config.QUALITY_AUDIO_ONLY -> context.getString(R.string.settings_quality_audio_only)
        "2160p" -> "4K (2160p)"
        "1080p" -> "1080p (Full HD)"
        "720p" -> "720p (HD)"
        "480p" -> "480p (SD)"
        "360p" -> "360p"
        else -> quality
    }

    fun getQualityOptions(context: Context, rawOptions: List<String>): List<Pair<String, String>> {
        return rawOptions.map { key -> key to getQualityLabel(context, key) }
    }

    fun getSpeedLabel(context: Context, speed: String): String = when (speed.lowercase()) {
        Config.SPEED_UNLIMITED -> context.getString(R.string.settings_speed_unlimited)
        else -> speed
    }

    fun getStorageMarginLabel(context: Context, margin: String): String = when (margin.lowercase()) {
        Config.STORAGE_MARGIN_DISABLED -> context.getString(R.string.settings_disabled)
        else -> margin
    }

    fun getStorageMarginOptions(context: Context, rawOptions: List<String>): List<Pair<String, String>> {
        return rawOptions.map { key -> key to getStorageMarginLabel(context, key) }
    }

    fun getBatteryActionLabel(context: Context, action: String): String = when (action.lowercase()) {
        Config.BATTERY_ACTION_OPTIMIZE -> context.getString(R.string.settings_battery_action_optimize)
        Config.BATTERY_ACTION_LIMIT -> context.getString(R.string.settings_battery_action_limit)
        else -> action
    }

    fun getBatteryActionOptions(context: Context, rawOptions: List<String>): List<Pair<String, String>> {
        return rawOptions.map { key -> key to getBatteryActionLabel(context, key) }
    }

    fun getClipboardActionLabel(context: Context, action: String): String = when (action.lowercase()) {
        Config.CLIPBOARD_ACTION_BANNER -> context.getString(R.string.settings_clipboard_banner)
        Config.CLIPBOARD_ACTION_AUTO -> context.getString(R.string.settings_clipboard_auto)
        Config.CLIPBOARD_ACTION_DISABLED -> context.getString(R.string.settings_clipboard_disabled)
        else -> action
    }

    fun getClipboardActionOptions(context: Context): List<Pair<String, String>> {
        return listOf(
            Config.CLIPBOARD_ACTION_BANNER to context.getString(R.string.settings_clipboard_banner),
            Config.CLIPBOARD_ACTION_AUTO to context.getString(R.string.settings_clipboard_auto),
            Config.CLIPBOARD_ACTION_DISABLED to context.getString(R.string.settings_clipboard_disabled)
        )
    }

    fun getCardStyleLabel(context: Context, style: String): String = when (style.lowercase()) {
        Config.CARD_STYLE_DETAILED -> context.getString(R.string.settings_card_style_detailed)
        Config.CARD_STYLE_COMPACT -> context.getString(R.string.settings_card_style_compact)
        Config.CARD_STYLE_MINIMAL -> context.getString(R.string.settings_card_style_minimal)
        else -> style
    }

    fun getCardStyleOptions(context: Context, rawOptions: List<String>): List<Pair<String, String>> {
        return rawOptions.map { key -> key to getCardStyleLabel(context, key) }
    }

    fun getAccentColorLabel(context: Context, colorKey: String): String = when (colorKey.lowercase()) {
        Config.ACCENT_ELECTRIC_BLUE -> context.getString(R.string.settings_accent_electric_blue)
        Config.ACCENT_EMERALD_GREEN -> context.getString(R.string.settings_accent_emerald_green)
        Config.ACCENT_ROYAL_PURPLE -> context.getString(R.string.settings_accent_royal_purple)
        Config.ACCENT_SUNSET_ORANGE -> context.getString(R.string.settings_accent_sunset_orange)
        Config.ACCENT_HOT_PINK -> context.getString(R.string.settings_accent_hot_pink)
        Config.ACCENT_STEEL_GRAY -> context.getString(R.string.settings_accent_steel_gray)
        else -> colorKey
    }

    fun getAccentColorOptions(context: Context, rawOptions: List<String>): List<Pair<String, String>> {
        return rawOptions.map { key -> key to getAccentColorLabel(context, key) }
    }

    fun getLanguageLabel(lang: String): String {
        return LocaleHelper.getDisplayName(lang)
    }
}
