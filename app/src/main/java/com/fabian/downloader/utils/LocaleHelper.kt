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
        "🇩🇪 Deutsch",
        "🇵🇹 Português",
        "🇮🇹 Italiano",
        "🇨🇳 中文",
        "🇰🇷 한국어",
        "🇸🇦 العربية",
        "🇮🇳 हिन्दी",
        "🇹🇷 Türkçe",
        "🇮🇩 Bahasa Indonesia",
        "🇻🇳 Tiếng Việt",
        "🇵🇱 Polski"
    )

    fun getLocaleForLanguage(lang: String): Locale {
        return when {
            lang.contains("English", ignoreCase = true) || lang.equals("en", ignoreCase = true) -> Locale.forLanguageTag("en")
            lang.contains("Español", ignoreCase = true) || lang.equals("es", ignoreCase = true) -> Locale.forLanguageTag("es")
            lang.contains("Français", ignoreCase = true) || lang.contains("French", ignoreCase = true) || lang.equals("fr", ignoreCase = true) -> Locale.forLanguageTag("fr")
            lang.contains("Русский", ignoreCase = true) || lang.contains("Russian", ignoreCase = true) || lang.equals("ru", ignoreCase = true) -> Locale.forLanguageTag("ru")
            lang.contains("日本語", ignoreCase = true) || lang.contains("Japanese", ignoreCase = true) || lang.equals("ja", ignoreCase = true) -> Locale.forLanguageTag("ja")
            lang.contains("Deutsch", ignoreCase = true) || lang.contains("German", ignoreCase = true) || lang.equals("de", ignoreCase = true) -> Locale.forLanguageTag("de")
            lang.contains("Português", ignoreCase = true) || lang.contains("Portuguese", ignoreCase = true) || lang.equals("pt", ignoreCase = true) -> Locale.forLanguageTag("pt")
            lang.contains("Italiano", ignoreCase = true) || lang.contains("Italian", ignoreCase = true) || lang.equals("it", ignoreCase = true) -> Locale.forLanguageTag("it")
            lang.contains("中文", ignoreCase = true) || lang.contains("Chinese", ignoreCase = true) || lang.equals("zh", ignoreCase = true) -> Locale.forLanguageTag("zh")
            lang.contains("한국어", ignoreCase = true) || lang.contains("Korean", ignoreCase = true) || lang.equals("ko", ignoreCase = true) -> Locale.forLanguageTag("ko")
            lang.contains("العربية", ignoreCase = true) || lang.contains("Arabic", ignoreCase = true) || lang.equals("ar", ignoreCase = true) -> Locale.forLanguageTag("ar")
            lang.contains("हिन्दी", ignoreCase = true) || lang.contains("Hindi", ignoreCase = true) || lang.equals("hi", ignoreCase = true) -> Locale.forLanguageTag("hi")
            lang.contains("Türkçe", ignoreCase = true) || lang.contains("Turkish", ignoreCase = true) || lang.equals("tr", ignoreCase = true) -> Locale.forLanguageTag("tr")
            lang.contains("Bahasa", ignoreCase = true) || lang.contains("Indonesian", ignoreCase = true) || lang.equals("id", ignoreCase = true) -> Locale.forLanguageTag("id")
            lang.contains("Tiếng Việt", ignoreCase = true) || lang.contains("Vietnamese", ignoreCase = true) || lang.equals("vi", ignoreCase = true) -> Locale.forLanguageTag("vi")
            lang.contains("Polski", ignoreCase = true) || lang.contains("Polish", ignoreCase = true) || lang.equals("pl", ignoreCase = true) -> Locale.forLanguageTag("pl")
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
            trimmed.equals("pt", ignoreCase = true) || trimmed.contains("Português", ignoreCase = true) -> "🇵🇹 Português"
            trimmed.equals("it", ignoreCase = true) || trimmed.contains("Italiano", ignoreCase = true) -> "🇮🇹 Italiano"
            trimmed.equals("zh", ignoreCase = true) || trimmed.contains("中文", ignoreCase = true) -> "🇨🇳 中文"
            trimmed.equals("ko", ignoreCase = true) || trimmed.contains("한국어", ignoreCase = true) -> "🇰🇷 한국어"
            trimmed.equals("ar", ignoreCase = true) || trimmed.contains("العربية", ignoreCase = true) -> "🇸🇦 العربية"
            trimmed.equals("hi", ignoreCase = true) || trimmed.contains("हिन्दी", ignoreCase = true) -> "🇮🇳 हिन्दी"
            trimmed.equals("tr", ignoreCase = true) || trimmed.contains("Türkçe", ignoreCase = true) -> "🇹🇷 Türkçe"
            trimmed.equals("id", ignoreCase = true) || trimmed.contains("Bahasa", ignoreCase = true) -> "🇮🇩 Bahasa Indonesia"
            trimmed.equals("vi", ignoreCase = true) || trimmed.contains("Tiếng Việt", ignoreCase = true) -> "🇻🇳 Tiếng Việt"
            trimmed.equals("pl", ignoreCase = true) || trimmed.contains("Polski", ignoreCase = true) -> "🇵🇱 Polski"
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
        
        // Fix for "BinderProxy cannot be cast to ClientTransaction" on Xiaomi/MIUI.
        // Returning createConfigurationContext() can break the context's identity in some OEM implementations.
        // Updating the resources in place and returning the exact original context is much safer here.
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        
        return context
    }
}
