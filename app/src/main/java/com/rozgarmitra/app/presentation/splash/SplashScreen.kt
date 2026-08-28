package com.rozgarmitra.app.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.RozgarRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (Boolean) -> Unit // Boolean indicates if language is already selected
) {
    var startAnimation by remember { mutableStateOf(false) }
    val isLanguageSelected by RozgarRepository.isLanguageSelected.collectAsState()

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onSplashFinished(isLanguageSelected)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(1000)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Circular Handshake Logo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Handshake,
                            contentDescription = "RozgarMitra Logo",
                            modifier = Modifier.size(70.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Branding Text
                    Text(
                        text = "RozgarMitra",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = "Bridging Hands, Building Future",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Tagline at bottom
        Text(
            text = "Made for Bharat",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
