package com.rozgarmitra.app.presentation.splash

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
import com.rozgarmitra.app.ui.theme.DarkBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (Boolean) -> Unit // Boolean indicates if language is already selected
) {
    val isLanguageSelected by RozgarRepository.isLanguageSelected.collectAsState()

    LaunchedEffect(Unit) {
        delay(3000L) // Single ~3-second custom splash timer
        onSplashFinished(isLanguageSelected)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground), // Dark navy #0F172A
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Circular Logo Symbol Box
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), CircleShape)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_symbol),
                    contentDescription = "RozgarMitra Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // RozgarMitra wordmark
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rozgar",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8), // Sky Blue
                    letterSpacing = 1.sp,
                    fontSize = 34.sp
                )
                Text(
                    text = "Mitra",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF34D399), // Emerald Green
                    letterSpacing = 1.sp,
                    fontSize = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // JOBS • WORKERS • OPPORTUNITIES
            Text(
                text = "JOBS  •  WORKERS  •  OPPORTUNITIES",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8), // Slate grey
                letterSpacing = 1.5.sp,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // "Bridging Hands, Building Future"
            Text(
                text = "Bridging Hands, Building Future",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE2E8F0), // Soft off-white
                letterSpacing = 0.5.sp,
                fontSize = 15.sp
            )
        }

        // Bottom Tagline: "Made for Bharat 🇮🇳"
        Text(
            text = "Made for Bharat 🇮🇳",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }
}

