package com.rozgarmitra.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rozgar_prefs", Context.MODE_PRIVATE)

    companion object {
        val LANGUAGE_CODE_MAP = mapOf(
            "English" to "en",
            "Hindi" to "hi",
            "Tamil" to "ta",
            "Telugu" to "te",
            "Kannada" to "kn",
            "Malayalam" to "ml",
            "Marathi" to "mr",
            "Gujarati" to "gu",
            "Bengali" to "bn",
            "Punjabi" to "pa",
            "Odia" to "or"
        )

        val CODE_TO_NAME_MAP = LANGUAGE_CODE_MAP.entries.associate { (k, v) -> v to k }

        fun mapLanguageNameToCode(name: String?): String {
            if (name == null) return "en"
            return LANGUAGE_CODE_MAP[name] ?: LANGUAGE_CODE_MAP.entries.firstOrNull { 
                it.key.equals(name, ignoreCase = true) 
            }?.value ?: if (name.length <= 3) name.lowercase() else "en"
        }

        fun mapCodeToLanguageName(code: String?): String {
            if (code == null) return "English"
            return CODE_TO_NAME_MAP[code.lowercase()] ?: CODE_TO_NAME_MAP.entries.firstOrNull {
                it.key.equals(code, ignoreCase = true)
            }?.value ?: "English"
        }
    }

    fun setLanguage(language: String, code: String? = null) {
        val langCode = code ?: mapLanguageNameToCode(language)
        prefs.edit()
            .putString("selected_language", language)
            .putString("selected_language_code", langCode)
            .apply()
    }

    fun getLanguage(): String? {
        return prefs.getString("selected_language", null)
    }

    fun getLanguageCode(): String {
        val code = prefs.getString("selected_language_code", null)
        if (!code.isNullOrBlank()) return code
        val lang = getLanguage() ?: return "en"
        return mapLanguageNameToCode(lang)
    }

    fun setLoggedInPhone(phone: String?) {
        prefs.edit().putString("logged_in_phone", phone).apply()
    }

    fun getLoggedInPhone(): String? {
        return prefs.getString("logged_in_phone", null)
    }

    fun setTheme(theme: ThemeMode) {
        prefs.edit().putString("selected_theme", theme.name).apply()
    }

    fun getTheme(): ThemeMode? {
        val name = prefs.getString("selected_theme", null) ?: return null
        return try { ThemeMode.valueOf(name) } catch (e: Exception) { null }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun getNotificationsEnabled(): Boolean {
        return prefs.getBoolean("notifications_enabled", true)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
