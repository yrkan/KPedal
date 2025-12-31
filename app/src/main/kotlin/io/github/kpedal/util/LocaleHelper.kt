package io.github.kpedal.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Helper for managing app locale/language settings.
 * Uses SharedPreferences for instant access on app startup.
 */
object LocaleHelper {

    private const val PREF_NAME = "kpedal_locale"
    private const val KEY_LANGUAGE = "selected_language"

    /**
     * Available app languages.
     * Order: System, then alphabetically by English name.
     */
    enum class AppLanguage(val code: String) {
        SYSTEM(""),
        ARABIC("ar"),
        CHINESE("zh"),
        DANISH("da"),
        DUTCH("nl"),
        ENGLISH("en"),
        FRENCH("fr"),
        GERMAN("de"),
        HEBREW("he"),
        ITALIAN("it"),
        JAPANESE("ja"),
        KOREAN("ko"),
        POLISH("pl"),
        PORTUGUESE("pt"),
        RUSSIAN("ru"),
        SPANISH("es"),
        SWEDISH("sv"),
        UKRAINIAN("uk")
    }

    /**
     * Save the selected language code to SharedPreferences.
     */
    fun saveLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    /**
     * Get the saved language from SharedPreferences.
     */
    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val languageName = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name)
        return try {
            AppLanguage.valueOf(languageName ?: AppLanguage.SYSTEM.name)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }
    }

    /**
     * Wrap the context with the selected locale configuration.
     * Call this from Activity.attachBaseContext()
     */
    fun wrapContext(context: Context): Context {
        val language = getSavedLanguage(context)
        return if (language == AppLanguage.SYSTEM) {
            context
        } else {
            val locale = getLocaleForLanguage(language)
            wrapContextWithLocale(context, locale)
        }
    }

    /**
     * Apply the selected language - saves preference and returns whether activity needs recreation.
     */
    fun applyLanguage(context: Context, language: AppLanguage): Boolean {
        val currentLanguage = getSavedLanguage(context)
        if (currentLanguage == language) {
            return false
        }
        saveLanguage(context, language)
        return true
    }

    /**
     * Get Locale for the given AppLanguage.
     */
    private fun getLocaleForLanguage(language: AppLanguage): Locale {
        return if (language == AppLanguage.SYSTEM || language.code.isEmpty()) {
            Locale.getDefault()
        } else {
            Locale(language.code)
        }
    }

    /**
     * Wrap context with the specified locale.
     * Sets layout direction for RTL languages (Hebrew, Arabic).
     */
    private fun wrapContextWithLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLayoutDirection(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    /**
     * Get current app locale.
     */
    fun getCurrentLocale(context: Context): Locale {
        val language = getSavedLanguage(context)
        return if (language == AppLanguage.SYSTEM) {
            Locale.getDefault()
        } else {
            getLocaleForLanguage(language)
        }
    }
}
