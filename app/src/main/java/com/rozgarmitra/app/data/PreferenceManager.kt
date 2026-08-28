package com.rozgarmitra.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rozgar_prefs", Context.MODE_PRIVATE)

    fun setLanguage(language: String) {
        prefs.edit().putString("selected_language", language).apply()
    }

    fun getLanguage(): String? {
        return prefs.getString("selected_language", null)
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

    fun clear() {
        prefs.edit().clear().apply()
    }
}
