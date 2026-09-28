package com.rozgarmitra.app.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.R
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (Boolean) -> Unit // Boolean indicates if language is already selected
) {
    var startAnimation by remember { mutableStateOf(false) }
    val isLanguageSelected by RozgarRepository.isLanguageSelected.collectAsState()

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2400) // Single unified ~2.4s total splash display
        onSplashFinished(isLanguageSelected)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(600)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Circular Logo Symbol Box
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, PrimaryBlue.copy(alpha = 0.4f), CircleShape)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo_symbol),
                            contentDescription = "RozgarMitra Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Branding Text
                    Text(
                        text = "RozgarMitra",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                    
                    Text(
                        text = "Bridging Hands, Building Future",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentCyan,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Bottom Tagline
        Text(
            text = "Made for Bharat 🇮🇳",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
