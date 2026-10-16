package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.R
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.ui.theme.*

data class LanguageItem(
    val code: String,
    val englishName: String,
    val nativeName: String,
)

val languagesList = listOf(
    LanguageItem("en", "English", "English"),
    LanguageItem("hi", "Hindi", "हिन्दी"),
    LanguageItem("ta", "Tamil", "தமிழ்"),
    LanguageItem("te", "Telugu", "తెలుగు"),
    LanguageItem("kn", "Kannada", "ಕನ್ನಡ"),
    LanguageItem("ml", "Malayalam", "മലയാളം"),
    LanguageItem("mr", "Marathi", "मराठी"),
    LanguageItem("gu", "Gujarati", "ગુજરાતી"),
    LanguageItem("bn", "Bengali", "বাংলা"),
    LanguageItem("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    LanguageItem("or", "Odia", "ଓଡ଼ିଆ")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectScreen(
    onLanguageSelected: () -> Unit
) {
    val currentLangCode by RozgarRepository.currentLanguageCode.collectAsState()
    var selectedCode by remember(currentLangCode) { mutableStateOf(currentLangCode) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBackground),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = null, tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.select_language_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.select_language_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(languagesList) { lang ->
                        val isSelected = lang.code == selectedCode
                        Card(
                            onClick = { selectedCode = lang.code },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else DarkSurface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryBlue else BorderStrokeColor
                            ),
                            modifier = Modifier
                                .height(80.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = lang.englishName,
                                    fontSize = 12.sp,
                                    color = if (isSelected) AccentCyan else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryLargeButton(
                    text = stringResource(R.string.confirm_continue),
                    onClick = { 
                        RozgarRepository.setLanguage(selectedCode)
                        onLanguageSelected()
                    }
                )
            }
        }
    }
}
