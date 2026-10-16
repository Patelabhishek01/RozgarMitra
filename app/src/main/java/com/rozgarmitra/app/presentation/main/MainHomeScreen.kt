package com.rozgarmitra.app.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.data.User
import com.rozgarmitra.app.presentation.components.*
import com.rozgarmitra.app.ui.theme.*

import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToJobDetails: (String) -> Unit,
    onNavigateToSeeAll: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfessions: () -> Unit,
    onNavigateToApplications: () -> Unit,
    onNavigateToWorkerHome: () -> Unit,
    onNavigateToOwnerHome: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    
    // Auto-redirect if logged in
    LaunchedEffect(currentUser) {
        currentUser?.let {
            if (it.role == Role.LABOUR) onNavigateToWorkerHome()
            else if (it.role == Role.OWNER) onNavigateToOwnerHome()
        }
    }

    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            if (isOffline) {
                OfflineBanner(isOffline = true)
            }
        },
        bottomBar = {
            val navItems = listOf(
                RozgarNavItem(
                    label = stringResource(R.string.nav_home),
                    icon = Icons.Filled.Home,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ),
                RozgarNavItem(
                    label = stringResource(R.string.nav_search),
                    icon = Icons.Filled.Search,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ),
                RozgarNavItem(
                    label = stringResource(R.string.nav_chats),
                    icon = Icons.Filled.Chat,
                    selected = selectedTab == 2,
                    onClick = {
                        if (currentUser == null) {
                            RozgarRepository.setPendingAction { selectedTab = 2 }
                            onNavigateToLogin()
                        } else {
                            selectedTab = 2
                        }
                    }
                ),
                RozgarNavItem(
                    label = stringResource(R.string.nav_profile),
                    icon = Icons.Filled.Person,
                    selected = selectedTab == 3,
                    onClick = {
                        if (currentUser == null) {
                            RozgarRepository.setPendingAction { selectedTab = 3 }
                            onNavigateToLogin()
                        } else {
                            selectedTab = 3
                        }
                    }
                )
            )
            RozgarBottomNavBar(items = navItems)
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(DarkBackground)) {
            when (selectedTab) {
                0 -> HomeFeedTab(
                    onJobClick = onNavigateToJobDetails,
                    onApplyClick = {
                        RozgarRepository.setPendingAction { /* Logic to resume apply */ }
                        onNavigateToLogin()
                    },
                    onNavigateToLogin = onNavigateToLogin,
                    onSeeAllUrgent = { onNavigateToSeeAll("Urgent Jobs", "urgent") },
                    onSeeAllNearby = { onNavigateToSeeAll("Nearby Jobs", "nearby") },
                    onSeeAllPopular = { onNavigateToSeeAll("Popular Jobs", "popular") },
                    onNotificationClick = { showNotifDialog = true }
                )
                1 -> SearchTab(
                    onJobClick = onNavigateToJobDetails,
                    onApplyClick = {
                        RozgarRepository.setPendingAction { /* Logic to resume apply */ }
                        onNavigateToLogin()
                    }
                )
                2 -> {
                    // This is reached only if logged in
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Chat, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(32.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.no_messages_yet), color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                3 -> ProfileTab(
                    user = currentUser,
                    onLoginClick = onNavigateToLogin,
                    onSettingsClick = onNavigateToSettings,
                    onProfessionsClick = onNavigateToProfessions,
                    onApplicationsClick = onNavigateToApplications,
                    onLogoutClick = { RozgarRepository.logout() }
                )
            }
        }
    }

    if (showNotifDialog) {
        com.rozgarmitra.app.presentation.components.NotificationsDialog(
            notifications = notifications,
            onDismiss = { showNotifDialog = false },
            onNotificationClick = { notif ->
                try {
                    if (notif.relatedJobId.isNotBlank()) {
                        onNavigateToJobDetails(notif.relatedJobId)
                    }
                    scope.launch {
                        val res = RozgarRepository.markNotificationRead(notif.id)
                        if (res.isFailure) {
                            android.util.Log.e("MainHomeScreen", "Failed to mark notification read in Firestore: ${notif.id}", res.exceptionOrNull())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainHomeScreen", "Failed to navigate for notification: ${notif.id}", e)
                }
            }
        )
    }
}

@Composable
fun ProfileTab(
    user: User?,
    onLoginClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfessionsClick: () -> Unit,
    onApplicationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
        item {
            ProfileHeader(user, onLoginClick)
        }
        
        item {
            ProfileSection(title = stringResource(R.string.section_account)) {
                ProfileMenuItem(icon = Icons.Filled.ListAlt, title = stringResource(R.string.menu_my_applications), onClick = onApplicationsClick)
                ProfileMenuItem(icon = Icons.Filled.Work, title = stringResource(R.string.menu_my_professions), onClick = onProfessionsClick)
            }
        }

        item {
            ProfileSection(title = stringResource(R.string.section_settings_support)) {
                ProfileMenuItem(icon = Icons.Filled.Settings, title = stringResource(R.string.menu_settings), onClick = onSettingsClick)
                ProfileMenuItem(icon = Icons.Filled.Help, title = stringResource(R.string.menu_help_support), onClick = { /* TODO */ })
            }
        }

        if (user != null) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRose)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_logout), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User?, onLoginClick: () -> Unit) {
    Surface(
        color = DarkSurface,
        border = BorderStroke(1.dp, BorderStrokeColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryIndigo.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(40.dp),
                    tint = PrimaryIndigo
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (user != null) {
                Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(user.phone, fontSize = 14.sp, color = TextSecondary)
                
                if (user.isVerified) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = AccentCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.verified_badge), fontSize = 12.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.greeting_namaste), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(stringResource(R.string.enter_details_start), fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLoginClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
                ) {
                    Text(stringResource(R.string.action_login), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderStrokeColor)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryIndigo.copy(alpha = 0.12f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}

