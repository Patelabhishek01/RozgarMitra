package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton

data class LanguageItem(
    val englishName: String,
    val nativeName: String,
)

val languagesList = listOf(
    LanguageItem("English", "English"),
    LanguageItem("Hindi", "हिन्दी"),
    LanguageItem("Tamil", "தமிழ்"),
    LanguageItem("Telugu", "తెలుగు"),
    LanguageItem("Kannada", "ಕನ್ನಡ"),
    LanguageItem("Malayalam", "മലയാളം"),
    LanguageItem("Marathi", "मराठी"),
    LanguageItem("Gujarati", "ગુજરાતી"),
    LanguageItem("Bengali", "বাংলা"),
    LanguageItem("Punjabi", "ਪੰਜਾਬੀ"),
    LanguageItem("Odia", "ଓଡ଼ିଆ")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectScreen(
    onLanguageSelected: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("English") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Language", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "चुनें अपनी भाषा / Select your language",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(languagesList) { lang ->
                        val isSelected = lang.englishName == selectedLanguage
                        Card(
                            onClick = { selectedLanguage = lang.englishName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .height(80.dp) // Large tap targets!
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
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = lang.englishName,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(20.dp))
                PrimaryLargeButton(
                    text = "Confirm / आगे बढ़ें",
                    onClick = { 
                        RozgarRepository.setLanguage(selectedLanguage)
                        onLanguageSelected()
                    }
                )
            }
        }
    }
}
