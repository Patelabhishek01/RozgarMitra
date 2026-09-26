package com.rozgarmitra.app.presentation.main

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
import com.rozgarmitra.app.presentation.components.HomeFeedTab
import com.rozgarmitra.app.presentation.components.OfflineBanner
import com.rozgarmitra.app.presentation.components.SearchTab

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
    var selectedTab by remember { mutableStateOf(0) }
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
    
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isOffline) {
                OfflineBanner(isOffline = true)
            }
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                containerColor = Color.White
            ) {
                val tabs = listOf(
                    Triple("Home", Icons.Filled.Home, 0),
                    Triple("Search", Icons.Filled.Search, 1),
                    Triple("Chats", Icons.Filled.Chat, 2),
                    Triple("Profile", Icons.Filled.Person, 3)
                )
                
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index >= 2 && currentUser == null) {
                                RozgarRepository.setPendingAction { selectedTab = index }
                                onNavigateToLogin()
                            } else {
                                selectedTab = index
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFFF8F9FA))) {
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
                        Text("Your messages will appear here", color = Color.Gray)
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
        // Notification dialog removed as it is now in Settings or TopBar
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ProfileHeader(user, onLoginClick)
        }
        
        item {
            ProfileSection(title = "Account") {
                ProfileMenuItem(icon = Icons.Filled.ListAlt, title = "My Applications", onClick = onApplicationsClick)
                ProfileMenuItem(icon = Icons.Filled.Work, title = "My Professions", onClick = onProfessionsClick)
                ProfileMenuItem(icon = Icons.Filled.Bookmark, title = "Saved Jobs", onClick = { /* TODO */ })
            }
        }

        item {
            ProfileSection(title = "Preferences & Support") {
                ProfileMenuItem(icon = Icons.Filled.Settings, title = "Settings", onClick = onSettingsClick)
                ProfileMenuItem(icon = Icons.Filled.Help, title = "Help Center", onClick = { /* TODO */ })
                ProfileMenuItem(icon = Icons.Filled.Share, title = "Invite Friends", onClick = { /* TODO */ })
            }
        }

        if (user != null) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User?, onLoginClick: () -> Unit) {
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person, 
                    contentDescription = null, 
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (user != null) {
                Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(user.phone, fontSize = 14.sp, color = Color.Gray)
                
                if (user.isVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified Profile", fontSize = 12.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text("Namaste!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Login to save jobs and apply", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text("Login / Register", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Surface(color = Color.White) {
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
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}
