package com.rozgarmitra.app.presentation.worker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.*
import com.rozgarmitra.app.presentation.components.JobFeedCard
import com.rozgarmitra.app.presentation.components.OfflineBanner
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.ProfileMenuItem
import com.rozgarmitra.app.presentation.components.ProfileSection
import com.rozgarmitra.app.presentation.components.VerifiedBadge
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourHomeScreen(
    onJobClick: (String) -> Unit,
    onChatThreadClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onProfessionsClick: () -> Unit,
    onApplicationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val unreadNotifs = notifications.count { !it.isRead }
    
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column {
                OfflineBanner(isOffline = isOffline)
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkSurface),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RozgarMitra", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryIndigo)
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    "WORKER / LABOUR", 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = SuccessGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNotifDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = WarningRose) { Text(unreadNotifs.toString(), color = Color.White) }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextPrimary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, BorderStrokeColor)
            ) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = DarkSurface,
                    modifier = Modifier.height(72.dp)
                ) {
                    val items = listOf(
                        Triple("Home", Icons.Filled.Home, 0),
                        Triple("Search", Icons.Filled.Search, 1),
                        Triple("Chats", Icons.Filled.Chat, 2),
                        Triple("Profile", Icons.Filled.Person, 3)
                    )
                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                            label = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryIndigo,
                                selectedTextColor = PrimaryIndigo,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            when (selectedTab) {
                0 -> LabourHomeFeedTab(onJobClick = onJobClick)
                1 -> LabourSearchTab(onJobClick = onJobClick)
                2 -> LabourChatsTab(onChatThreadClick = onChatThreadClick)
                3 -> LabourProfileTab(
                    onSettingsClick = onSettingsClick,
                    onProfessionsClick = onProfessionsClick,
                    onApplicationsClick = onApplicationsClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            containerColor = DarkSurface,
            title = { Text("Notifications", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                if (notifications.isEmpty()) {
                    Text("No new notifications.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                                border = BorderStroke(1.dp, BorderStrokeColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.message, fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                            RozgarRepository.markNotificationRead(notif.id)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("Dismiss", fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                }
            }
        )
    }
}

// --- SUB TABS FOR HOME (FEED & APPLICATIONS) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourHomeFeedTab(onJobClick: (String) -> Unit) {
    var subTabState by remember { mutableStateOf(0) } // 0 = Feed, 1 = Applications
    
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val userLocationData by RozgarRepository.userLocationData.collectAsStateWithLifecycle()
    val searchRadiusKm by RozgarRepository.searchRadiusKm.collectAsStateWithLifecycle()

    var showLocationDialog by remember { mutableStateOf(false) }

    val displayLocation = userLocationData?.addressName?.ifBlank { "Location Available" } ?: "Select Location"

    val filteredJobs = jobs.filter { job ->
        searchRadiusKm == 0.0 || job.distanceKm == 0.0 || job.distanceKm <= searchRadiusKm
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = subTabState,
            containerColor = DarkSurface,
            contentColor = PrimaryIndigo,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[subTabState]),
                    color = PrimaryIndigo,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = subTabState == 0,
                onClick = { subTabState = 0 },
                text = { Text("Nearby Jobs", fontWeight = FontWeight.Bold, color = if (subTabState == 0) PrimaryIndigo else TextSecondary) }
            )
            Tab(
                selected = subTabState == 1,
                onClick = { subTabState = 1 },
                text = { 
                    BadgedBox(badge = {
                        val pending = applications.count { it.status == ApplicationStatus.APPLIED }
                        if (pending > 0) {
                            Badge(containerColor = PrimaryIndigo) { Text(pending.toString(), color = Color.White) }
                        }
                    }) {
                        Text("My Applications", fontWeight = FontWeight.Bold, color = if (subTabState == 1) PrimaryIndigo else TextSecondary)
                    }
                }
            )
        }

        if (subTabState == 0) {
            // FEED TAB
            Column(modifier = Modifier.fillMaxSize()) {
                com.rozgarmitra.app.presentation.components.LocationIndicator(
                    location = displayLocation,
                    radiusKm = searchRadiusKm,
                    onClick = { showLocationDialog = true }
                )

                if (filteredJobs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CustomIcon(Icons.Filled.WorkOutline, contentDescription = null, size = 32, tint = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No active jobs found nearby.", color = TextSecondary, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { RozgarRepository.observeJobs() }) {
                                Text("Refresh Feed", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredJobs, key = { it.id.ifBlank { it.title + it.createdAt } }) { job ->
                            JobFeedCard(
                                job = job, 
                                onClick = { onJobClick(job.id) },
                                onApplyClick = { /* Handled in card */ }
                            )
                        }
                    }
                }
            }
        } else {
            // APPLICATIONS TAB
            if (applications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CustomIcon(Icons.Filled.ListAlt, contentDescription = null, size = 32, tint = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("You haven't applied to any jobs yet.", color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applications.asReversed(), key = { it.id }) { app ->
                        ApplicationStatusCard(app = app, onJobClick = onJobClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomIcon(imageVector: ImageVector, contentDescription: String?, size: Int, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size.dp), tint = tint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationStatusCard(app: JobApplication, onJobClick: (String) -> Unit) {
    val (statusText, badgeColor) = when (app.status) {
        ApplicationStatus.APPLIED -> "Applied (ओटीपी भेजा)" to Color(0xFFF59E0B) // Amber
        ApplicationStatus.ACCEPTED -> "Hired / Approved (काम मिला)" to SuccessGreen
        ApplicationStatus.COMPLETED -> "Completed (पूर्ण हुआ)" to AccentCyan
        ApplicationStatus.REJECTED -> "Rejected (अस्वीकृत)" to WarningRose
        ApplicationStatus.CANCELLED -> "Cancelled (रद्द किया)" to TextMuted
    }

    Card(
        onClick = { onJobClick(app.jobId) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.jobTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                    shape = CircleShape
                ) {
                    Text(statusText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Work, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skills: ${app.labourSkills.joinToString(", ")}", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

// --- SEARCH & FILTER TAB ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourSearchTab(onJobClick: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDistance by remember { mutableStateOf<Double?>(null) }
    var selectedWage by remember { mutableStateOf<Int?>(null) }
    
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()

    val filteredJobs = jobs.filter { job ->
        val matchesQuery = job.title.lowercase().contains(searchQuery.lowercase()) ||
                job.category.lowercase().contains(searchQuery.lowercase()) ||
                job.location.lowercase().contains(searchQuery.lowercase())
        
        val category = selectedCategory
        val matchesCategory = category == null || job.category == category
        
        val distance = selectedDistance
        val matchesDistance = distance == null || job.distanceKm <= distance
        
        val wage = selectedWage
        val matchesWage = wage == null || job.wage >= wage
        
        matchesQuery && matchesCategory && matchesDistance && matchesWage
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by trade, area, or keywords...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryIndigo) },
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = DarkSurface,
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = BorderStrokeColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal filter chips
        Text("Quick Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category Filter
            FilterChip(
                selected = selectedCategory != null,
                onClick = {
                    selectedCategory = if (selectedCategory == null) "Mason" else null
                },
                label = { Text("Mason") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryIndigo,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderStrokeColor,
                    selectedBorderColor = PrimaryIndigo
                )
            )

            FilterChip(
                selected = selectedDistance != null,
                onClick = {
                    selectedDistance = if (selectedDistance == null) 5.0 else null
                },
                label = { Text("Under 5 km") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryIndigo,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderStrokeColor,
                    selectedBorderColor = PrimaryIndigo
                )
            )

            FilterChip(
                selected = selectedWage != null,
                onClick = {
                    selectedWage = if (selectedWage == null) 700 else null
                },
                label = { Text("₹700+ / Day") },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryIndigo,
                    selectedLabelColor = Color.White,
                    containerColor = DarkSurface,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderStrokeColor,
                    selectedBorderColor = PrimaryIndigo
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredJobs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No matching jobs found.", color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredJobs) { job ->
                    JobFeedCard(
                        job = job, 
                        onClick = { onJobClick(job.id) },
                        onApplyClick = { /* Already logged in */ }
                    )
                }
            }
        }
    }
}

// --- CHATS TAB ---

@Composable
fun LabourChatsTab(onChatThreadClick: (String) -> Unit) {
    val threads by RozgarRepository.threads.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()

    if (threads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = DarkSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("No active job chats yet.", color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(threads) { thread ->
                val displayName = thread.getOtherUserName(currentUser?.id ?: "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatThreadClick(thread.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrimaryIndigo.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PrimaryIndigo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            if (thread.otherUserVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 14)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(thread.lastMessageText, fontSize = 13.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (thread.unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(thread.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Divider(color = BorderStrokeColor.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// --- PROFILE & SETTINGS TAB ---

@Composable
fun LabourProfileTab(
    onSettingsClick: () -> Unit,
    onProfessionsClick: () -> Unit,
    onApplicationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            // Profile Header Card
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
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = PrimaryIndigo)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(currentUser?.name ?: "Worker", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    Text(currentUser?.phone ?: "", color = TextSecondary, fontSize = 14.sp)
                    
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            "WORKER / LABOUR", 
                            color = SuccessGreen, 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            ProfileSection(title = "Work & Applications") {
                ProfileMenuItem(
                    icon = Icons.Filled.Work,
                    title = "My Professions",
                    subtitle = "Manage your skills and categories",
                    onClick = onProfessionsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.ListAlt,
                    title = "My Applications",
                    subtitle = "Check status of jobs you applied for",
                    onClick = onApplicationsClick
                )
            }
        }

        item {
            ProfileSection(title = "Availability & Trust") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.EventAvailable, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Available for Work", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                            Text("Show your profile to employers", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    Switch(
                        checked = currentUser?.labourProfile?.isAvailable == true,
                        onCheckedChange = { RozgarRepository.toggleAvailability(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryIndigo,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
                
                ProfileMenuItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = if (currentUser?.isVerified == true) "Aadhaar Verified" else "Verify Aadhaar",
                    subtitle = if (currentUser?.isVerified == true) "Badge granted" else "Get a verification badge",
                    titleColor = if (currentUser?.isVerified == true) SuccessGreen else TextPrimary,
                    onClick = { /* TODO: Verification flow */ }
                )
            }
        }

        item {
            ProfileSection(title = "Settings & Support") {
                ProfileMenuItem(
                    icon = Icons.Filled.Settings,
                    title = "Settings",
                    subtitle = "Language, Appearance, Notifications",
                    onClick = onSettingsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Help,
                    title = "Help & Support",
                    onClick = { /* TODO */ }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.BugReport,
                    title = "Offline Mode",
                    subtitle = "Simulate offline state",
                    onClick = { RozgarRepository.toggleOffline(!isOffline) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedButton(
                    onClick = {
                        RozgarRepository.logout()
                        onLogoutClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRose)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout Account", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

