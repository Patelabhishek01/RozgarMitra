package com.rozgarmitra.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.data.ThemeMode
import com.rozgarmitra.app.presentation.navigation.AppNavGraph
import com.rozgarmitra.app.ui.theme.RozgarMitraTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        RozgarRepository.initialize(applicationContext)
        
        setContent {
            MainContent()
        }
    }
}

@Composable
private fun MainContent() {
    val themeMode by RozgarRepository.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val currentLangCode by RozgarRepository.currentLanguageCode.collectAsStateWithLifecycle()
    val locale = remember(currentLangCode) {
        Locale(currentLangCode).also {
            Locale.setDefault(it)
        }
    }
    val baseConfig = LocalConfiguration.current
    val configuration = remember(currentLangCode, baseConfig) {
        Configuration(baseConfig).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val context = LocalContext.current
    val localizedContext = remember(currentLangCode, context) {
        context.createConfigurationContext(configuration)
    }

    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalContext provides localizedContext
    ) {
        RozgarMitraTheme(darkTheme = darkTheme) {
            AppNavGraph()
        }
    }
}
