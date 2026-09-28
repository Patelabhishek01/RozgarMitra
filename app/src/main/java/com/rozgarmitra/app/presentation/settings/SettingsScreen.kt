package com.rozgarmitra.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.data.ThemeMode
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToProfessions: () -> Unit,
    onNavigateToHelpSupport: () -> Unit = {}
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val themeMode by RozgarRepository.themeMode.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            item { SettingsSectionHeader("Account") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Edit,
                    title = "Edit Profile Name",
                    subtitle = currentUser?.name?.ifBlank { "Set your display name" } ?: "Set your display name",
                    onClick = { showEditNameDialog = true }
                )
            }
            if (currentUser?.role == Role.LABOUR) {
                item {
                    SettingsItem(
                        icon = Icons.Filled.Person,
                        title = "My Professions",
                        subtitle = "Manage your work categories & wage",
                        onClick = onNavigateToProfessions
                    )
                }
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Lock,
                    title = "Account Details",
                    subtitle = "Role: ${currentUser?.role?.name ?: "Guest"} • ${currentUser?.phone?.ifBlank { currentUser?.id } ?: "Logged in"}",
                    onClick = { showEditNameDialog = true }
                )
            }

            item { SettingsSectionHeader("Preferences") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Translate,
                    title = "Language",
                    subtitle = "Current: ${currentUser?.language ?: "English"}",
                    onClick = onNavigateToLanguage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = "Mode: ${themeMode.name}",
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    subtitle = if (currentUser?.notificationsEnabled == false) "Disabled" else "Enabled",
                    onClick = { showNotificationDialog = true }
                )
            }

            item { SettingsSectionHeader("Support & Info") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Help,
                    title = "Help & FAQ",
                    subtitle = "Guides and common questions",
                    onClick = onNavigateToHelpSupport
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Email,
                    title = "Contact Support",
                    subtitle = "Email helpline and phone support",
                    onClick = onNavigateToHelpSupport
                )
            }

            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "About RozgarMitra",
                    subtitle = "Version 1.0",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(currentUser?.name ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { if (!isSaving) showEditNameDialog = false },
            title = { Text("Edit Profile Name", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your display name. This will be visible to other users.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStrokeColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                RozgarRepository.updateProfileName(nameInput.trim()).collectLatest {
                                    isSaving = false
                                    showEditNameDialog = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (isSaving) "Saving..." else "Save", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showNotificationDialog) {
        var enabledState by remember { mutableStateOf(currentUser?.notificationsEnabled != false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("In-App Notification Settings", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("In-App Notifications", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Receive application updates & chat alerts", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = enabledState,
                        onCheckedChange = {
                            enabledState = it
                            scope.launch {
                                RozgarRepository.setNotificationsEnabled(it).collectLatest {}
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Done", color = AccentCyan)
                }
            }
        )
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

    if (showAboutDialog) {
        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { showAboutDialog = false },
            title = { Text("RozgarMitra", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Version 1.0 (Direct Release)", color = AccentCyan, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Connecting local daily wage workers with project owners efficiently.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = PrimaryBlue)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = AccentCyan,
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
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, color = TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
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
        containerColor = DarkSurfaceElevated,
        onDismissRequest = onDismiss,
        title = { Text("Select Appearance", color = TextPrimary) },
        text = {
            Column {
                ThemeOption("Light", ThemeMode.LIGHT, currentMode == ThemeMode.LIGHT, onSelect)
                ThemeOption("Dark", ThemeMode.DARK, currentMode == ThemeMode.DARK, onSelect)
                ThemeOption("System Default", ThemeMode.SYSTEM, currentMode == ThemeMode.SYSTEM, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AccentCyan) }
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
        RadioButton(
            selected = isSelected,
            onClick = { onSelect(mode) },
            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue, unselectedColor = TextSecondary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = TextPrimary)
    }
}
