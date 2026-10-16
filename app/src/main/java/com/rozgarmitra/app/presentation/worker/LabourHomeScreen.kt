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
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
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
import com.rozgarmitra.app.presentation.components.*
import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.presentation.common.getApplicationStatusDisplayName
import com.rozgarmitra.app.presentation.common.getTradeDisplayName
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
    onHelpSupportClick: () -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    val scope = rememberCoroutineScope()
    
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val unreadNotifs = notifications.count { !it.isRead }
    val unreadAppNotifs = notifications.count { !it.isRead && RozgarRepository.isApplicationNotification(it) }
    
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
                                    stringResource(R.string.worker_badge), 
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
                                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notifications_title), tint = TextPrimary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                )
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
                    onClick = { selectedTab = 2 }
                ),
                RozgarNavItem(
                    label = stringResource(R.string.nav_profile),
                    icon = Icons.Filled.Person,
                    selected = selectedTab == 3,
                    badgeCount = unreadAppNotifs,
                    onClick = { selectedTab = 3 }
                )
            )
            RozgarBottomNavBar(items = navItems)
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
                    onHelpSupportClick = onHelpSupportClick,
                    onCompleteProfileClick = onCompleteProfileClick,
                    onLogoutClick = onLogoutClick
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
                    when (notif.type) {
                        com.rozgarmitra.app.data.NotificationType.NEW_MESSAGE.name -> {
                            if (notif.relatedThreadId.isNotBlank()) {
                                onChatThreadClick(notif.relatedThreadId)
                            } else {
                                selectedTab = 2
                            }
                        }
                        com.rozgarmitra.app.data.NotificationType.APPLICATION_ACCEPTED.name,
                        com.rozgarmitra.app.data.NotificationType.APPLICATION_REJECTED.name,
                        com.rozgarmitra.app.data.NotificationType.JOB_APPLICATION.name -> {
                            if (notif.relatedJobId.isNotBlank()) {
                                onJobClick(notif.relatedJobId)
                            } else {
                                onApplicationsClick()
                            }
                        }
                        else -> {
                            if (notif.relatedJobId.isNotBlank()) {
                                onJobClick(notif.relatedJobId)
                            } else if (notif.relatedApplicationId.isNotBlank()) {
                                onApplicationsClick()
                            }
                        }
                    }
                    scope.launch {
                        val res = RozgarRepository.markNotificationRead(notif.id)
                        if (res.isFailure) {
                            android.util.Log.e("LabourHomeScreen", "Failed to mark notification read in Firestore: ${notif.id}", res.exceptionOrNull())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LabourHomeScreen", "Failed to navigate for notification: ${notif.id}", e)
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
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val userLocationData by RozgarRepository.userLocationData.collectAsStateWithLifecycle()
    val searchRadiusKm by RozgarRepository.searchRadiusKm.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val unreadAppNotifs = notifications.count { !it.isRead && RozgarRepository.isApplicationNotification(it) }

    var showLocationDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val displayLocation = userLocationData?.addressName?.ifBlank { "Location Available" } ?: "Select Location"
    val isLocationAvailable = userLocationData != null && (userLocationData?.latitude != 0.0 || userLocationData?.longitude != 0.0)

    LaunchedEffect(Unit) {
        if (userLocationData == null) {
            val finePerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            val coarsePerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (finePerm == android.content.pm.PackageManager.PERMISSION_GRANTED || coarsePerm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val locData = LocationHelper.getCurrentLocation(context)
                if (locData != null) {
                    RozgarRepository.updateUserLocation(locData)
                    RozgarRepository.recalculateJobDistances()
                }
            }
        }
    }

    val filteredJobs = jobs.filter { job ->
        if (searchRadiusKm == 0.0) {
            true
        } else if (!isLocationAvailable) {
            true
        } else {
            job.distanceKm > 0.0 && job.distanceKm <= searchRadiusKm
        }
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
                text = { Text(stringResource(R.string.tab_nearby_jobs), fontWeight = FontWeight.Bold, color = if (subTabState == 0) PrimaryIndigo else TextSecondary) }
            )
            Tab(
                selected = subTabState == 1,
                onClick = { subTabState = 1 },
                text = { 
                    BadgedBox(badge = {
                        if (unreadAppNotifs > 0) {
                            Badge(containerColor = PrimaryIndigo) { Text(unreadAppNotifs.toString(), color = Color.White) }
                        }
                    }) {
                        Text(stringResource(R.string.tab_my_applications), fontWeight = FontWeight.Bold, color = if (subTabState == 1) PrimaryIndigo else TextSecondary)
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
                            Text(stringResource(R.string.no_active_jobs_nearby), color = TextSecondary, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { RozgarRepository.observeJobs() }) {
                                Text(stringResource(R.string.refresh_feed), color = PrimaryIndigo, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.no_applications_yet), color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(applications.asReversed(), key = { it.id }) { app ->
                        val appNotifs = notifications.filter { notif ->
                            !notif.isRead && (
                                notif.relatedApplicationId == app.id ||
                                (notif.relatedJobId == app.jobId && notif.type != NotificationType.NEW_MESSAGE.name)
                            )
                        }
                        ApplicationStatusCard(
                            app = app,
                            hasUnreadNotification = appNotifs.isNotEmpty(),
                            onJobClick = { jobId ->
                                if (appNotifs.isNotEmpty()) {
                                    scope.launch {
                                        appNotifs.forEach { notif ->
                                            val res = RozgarRepository.markNotificationRead(notif.id)
                                            if (res.isFailure) {
                                                android.util.Log.e("LabourHomeFeedTab", "Failed to mark notification read: ${notif.id}", res.exceptionOrNull())
                                            }
                                        }
                                    }
                                }
                                onJobClick(jobId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        com.rozgarmitra.app.presentation.components.LocationSelectionDialog(
            onDismiss = { showLocationDialog = false }
        )
    }
}


@Composable
private fun CustomIcon(imageVector: ImageVector, contentDescription: String?, size: Int, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size.dp), tint = tint)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationStatusCard(
    app: JobApplication, 
    hasUnreadNotification: Boolean = false,
    onJobClick: (String) -> Unit
) {
    val statusText = getApplicationStatusDisplayName(app.status.name)
    val badgeColor = when (app.status) {
        ApplicationStatus.APPLIED -> Color(0xFFF59E0B) // Amber
        ApplicationStatus.ACCEPTED -> SuccessGreen
        ApplicationStatus.COMPLETED -> AccentCyan
        ApplicationStatus.REJECTED -> WarningRose
        ApplicationStatus.CANCELLED -> TextMuted
    }

    Card(
        onClick = { onJobClick(app.jobId) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (hasUnreadNotification) PrimaryIndigo else BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(app.jobTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (hasUnreadNotification) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
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
                Text(stringResource(R.string.skills_required_label).substringBefore("(") + ": ${app.labourSkills.joinToString(", ")}", fontSize = 13.sp, color = TextSecondary)
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
            placeholder = { Text(stringResource(R.string.search_placeholder), color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.nav_search), tint = PrimaryIndigo) },
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
        Text(stringResource(R.string.filters_title), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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
                label = { Text(getTradeDisplayName("Mason")) },
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
                label = { Text(stringResource(R.string.under_5km)) },
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
                label = { Text(stringResource(R.string.wage_700_plus)) },
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
                Text(stringResource(R.string.no_matching_jobs), color = TextSecondary, fontWeight = FontWeight.Medium)
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
                Text(stringResource(R.string.no_active_chats_yet), color = TextSecondary, fontWeight = FontWeight.Medium)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourProfileTab(
    onSettingsClick: () -> Unit,
    onProfessionsClick: () -> Unit,
    onApplicationsClick: () -> Unit,
    onHelpSupportClick: () -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val unreadAppNotifs = notifications.count { !it.isRead && RozgarRepository.isApplicationNotification(it) }
    val scope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }

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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(currentUser?.name ?: "Worker", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Name", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(currentUser?.phone ?: currentUser?.id ?: "", color = TextSecondary, fontSize = 14.sp)
                    
                    Surface(
                        color = SuccessGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            stringResource(R.string.worker_badge), 
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
            if (currentUser?.profileCompleted != true) {
                Surface(
                    color = WarningRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onCompleteProfileClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningRose)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.complete_profile_title), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text(stringResource(R.string.complete_profile_desc), fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = WarningRose)
                    }
                }
            }
        }

        item {
            ProfileSection(title = stringResource(R.string.section_work_applications)) {
                ProfileMenuItem(
                    icon = Icons.Filled.Work,
                    title = stringResource(R.string.menu_my_professions),
                    subtitle = stringResource(R.string.menu_my_professions_subtitle),
                    onClick = onProfessionsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.ListAlt,
                    title = stringResource(R.string.menu_my_applications),
                    subtitle = stringResource(R.string.menu_my_applications_subtitle),
                    badgeCount = unreadAppNotifs,
                    onClick = onApplicationsClick
                )
            }
        }

        item {
            ProfileSection(title = stringResource(R.string.section_availability_trust)) {
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
                            Text(stringResource(R.string.available_for_work), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                            Text(stringResource(R.string.available_for_work_subtitle), fontSize = 12.sp, color = TextSecondary)
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
                
                val isVer = currentUser?.isVerified == true
                val isPen = currentUser?.verificationStatus == "PENDING"
                ProfileMenuItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = when {
                        isVer -> stringResource(R.string.aadhaar_verified)
                        isPen -> stringResource(R.string.verification_pending)
                        else -> stringResource(R.string.verify_aadhaar)
                    },
                    subtitle = when {
                        isVer -> stringResource(R.string.badge_granted)
                        isPen -> stringResource(R.string.under_review)
                        else -> stringResource(R.string.submit_id_verification)
                    },
                    titleColor = when {
                        isVer -> SuccessGreen
                        isPen -> AccentCyan
                        else -> TextPrimary
                    },
                    onClick = {
                        if (!isVer && !isPen) {
                            showVerificationDialog = true
                        }
                    }
                )
            }
        }

        item {
            ProfileSection(title = stringResource(R.string.section_settings_support)) {
                ProfileMenuItem(
                    icon = Icons.Filled.Settings,
                    title = stringResource(R.string.menu_settings),
                    subtitle = stringResource(R.string.menu_settings_subtitle),
                    onClick = onSettingsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Help,
                    title = stringResource(R.string.menu_help_support),
                    subtitle = stringResource(R.string.menu_help_support_subtitle),
                    onClick = onHelpSupportClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.BugReport,
                    title = stringResource(R.string.menu_offline_mode),
                    subtitle = if (isOffline) stringResource(R.string.offline_active) else stringResource(R.string.online_active),
                    titleColor = if (isOffline) WarningRose else TextPrimary,
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
                    Text(stringResource(R.string.action_logout_account), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showEditNameDialog) {
        var nameInput by remember { mutableStateOf(currentUser?.name ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { if (!isSaving) showEditNameDialog = false },
            title = { Text(stringResource(R.string.edit_profile_name), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.set_display_name), fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.full_name), color = TextSecondary) },
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
                    Text(if (isSaving) stringResource(R.string.action_loading) else stringResource(R.string.action_save), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }

    if (showVerificationDialog) {
        var selectedDocType by remember { mutableStateOf("Aadhaar Card") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { if (!isSubmitting) showVerificationDialog = false },
            title = { Text(stringResource(R.string.verify_aadhaar), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.submit_id_verification), fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("Aadhaar Card", "Voter ID", "Driving License").forEach { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDocType = doc }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDocType == doc,
                                onClick = { selectedDocType = doc },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(doc, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            RozgarRepository.submitVerificationRequest(selectedDocType).collectLatest {
                                isSubmitting = false
                                showVerificationDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (isSubmitting) stringResource(R.string.action_loading) else stringResource(R.string.action_submit), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }
}

