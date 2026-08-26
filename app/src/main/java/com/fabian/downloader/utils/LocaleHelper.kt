package com.fabian.downloader.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    val SUPPORTED_LANGUAGES = listOf(
        "🌐 Sistema",
        "🇪🇸 Español",
        "🇺🇸 English",
        "🇫🇷 Français",
        "🇷🇺 Русский",
        "🇯🇵 日本語",
        "🇩🇪 Deutsch"
    )

    fun getLocaleForLanguage(lang: String): Locale {
        return when {
            lang.contains("English", ignoreCase = true) || lang.equals("en", ignoreCase = true) -> Locale.forLanguageTag("en")
            lang.contains("Español", ignoreCase = true) || lang.equals("es", ignoreCase = true) -> Locale.forLanguageTag("es")
            lang.contains("Français", ignoreCase = true) || lang.contains("French", ignoreCase = true) || lang.equals("fr", ignoreCase = true) -> Locale.forLanguageTag("fr")
            lang.contains("Русский", ignoreCase = true) || lang.contains("Russian", ignoreCase = true) || lang.equals("ru", ignoreCase = true) -> Locale.forLanguageTag("ru")
            lang.contains("日本語", ignoreCase = true) || lang.contains("Japanese", ignoreCase = true) || lang.equals("ja", ignoreCase = true) -> Locale.forLanguageTag("ja")
            lang.contains("Deutsch", ignoreCase = true) || lang.contains("German", ignoreCase = true) || lang.equals("de", ignoreCase = true) -> Locale.forLanguageTag("de")
            else -> Locale.getDefault()
        }
    }

    fun getDisplayName(lang: String): String {
        val trimmed = lang.trim()
        val found = SUPPORTED_LANGUAGES.find {
            it.equals(trimmed, ignoreCase = true) ||
            it.contains(trimmed, ignoreCase = true)
        }
        if (found != null) return found

        return when {
            trimmed.equals("system", ignoreCase = true) || trimmed.isEmpty() -> "🌐 Sistema"
            trimmed.equals("es", ignoreCase = true) || trimmed.contains("Español", ignoreCase = true) -> "🇪🇸 Español"
            trimmed.equals("en", ignoreCase = true) || trimmed.contains("English", ignoreCase = true) -> "🇺🇸 English"
            trimmed.equals("fr", ignoreCase = true) || trimmed.contains("Français", ignoreCase = true) -> "🇫🇷 Français"
            trimmed.equals("ru", ignoreCase = true) || trimmed.contains("Русский", ignoreCase = true) -> "🇷🇺 Русский"
            trimmed.equals("ja", ignoreCase = true) || trimmed.contains("日本語", ignoreCase = true) -> "🇯🇵 日本語"
            trimmed.equals("de", ignoreCase = true) || trimmed.contains("Deutsch", ignoreCase = true) -> "🇩🇪 Deutsch"
            else -> "🌐 Sistema"
        }
    }

    fun applyLocale(context: Context, lang: String): Context {
        if (lang.contains("Sistema", ignoreCase = true)) {
            return context
        }
        val locale = getLocaleForLanguage(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
