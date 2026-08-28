package com.rozgarmitra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.data.ThemeMode
import com.rozgarmitra.app.presentation.navigation.AppNavGraph
import com.rozgarmitra.app.ui.theme.RozgarMitraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        RozgarRepository.initialize(this)

        setContent {
            val themeMode by RozgarRepository.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            RozgarMitraTheme(darkTheme = darkTheme) {
                AppNavGraph()
            }
        }
    }
}
