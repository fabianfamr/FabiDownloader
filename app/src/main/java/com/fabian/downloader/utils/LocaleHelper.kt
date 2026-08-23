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
