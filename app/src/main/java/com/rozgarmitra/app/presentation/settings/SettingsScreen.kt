package com.rozgarmitra.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToProfessions: () -> Unit
) {
    val themeMode by RozgarRepository.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { SettingsSectionHeader("Account") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    title = "My Professions",
                    subtitle = "Manage your work categories",
                    onClick = onNavigateToProfessions
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Lock,
                    title = "Login & Security",
                    subtitle = "Phone number, verification status",
                    onClick = { /* TODO */ }
                )
            }

            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Translate,
                    title = "Language",
                    subtitle = "Select your preferred language",
                    onClick = onNavigateToLanguage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = "Light, Dark, or System",
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = "Manage alerts and updates",
                    onClick = { /* TODO */ }
                )
            }

            item { SettingsSectionHeader("Support") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Help,
                    title = "Help & FAQ",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Email,
                    title = "Contact Support",
                    onClick = { /* TODO */ }
                )
            }

            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About RozgarMitra",
                    onClick = { /* TODO */ }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Description,
                    title = "Terms & Conditions",
                    onClick = { /* TODO */ }
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                RozgarRepository.setTheme(mode)
                showThemeDialog = false
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Appearance") },
        text = {
            Column {
                ThemeOption("Light", ThemeMode.LIGHT, currentMode == ThemeMode.LIGHT, onSelect)
                ThemeOption("Dark", ThemeMode.DARK, currentMode == ThemeMode.DARK, onSelect)
                ThemeOption("System Default", ThemeMode.SYSTEM, currentMode == ThemeMode.SYSTEM, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ThemeOption(
    label: String,
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(mode) }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = { onSelect(mode) })
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}
