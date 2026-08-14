package com.rozgarmitra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rozgarmitra.app.presentation.navigation.AppNavGraph
import com.rozgarmitra.app.ui.theme.RozgarMitraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RozgarMitraTheme {
                AppNavGraph()
            }
        }
    }
}