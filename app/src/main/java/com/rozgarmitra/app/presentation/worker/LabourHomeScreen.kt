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
import com.rozgarmitra.app.presentation.components.VerifiedBadge
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourHomeScreen(
    onJobClick: (String) -> Unit,
    onChatThreadClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val unreadNotifs = notifications.count { !it.isRead }
    
    var showNotifDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                OfflineBanner(isOffline = isOffline)
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RozgarMitra", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                            if (currentUser?.isVerified == true) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 18)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNotifDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge { Text(unreadNotifs.toString()) }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp) // Large targets for navigation items
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
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> LabourHomeFeedTab(onJobClick = onJobClick)
                1 -> LabourSearchTab(onJobClick = onJobClick)
                2 -> LabourChatsTab(onChatThreadClick = onChatThreadClick)
                3 -> LabourProfileTab(onLogoutClick = onLogoutClick)
            }
        }
    }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            text = {
                if (notifications.isEmpty()) {
                    Text("No new notifications.", color = Color.Gray)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(notif.message, fontSize = 12.sp, color = Color.DarkGray)
                                }
                            }
                            // Mark as read immediately on display
                            RozgarRepository.markNotificationRead(notif.id)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifDialog = false }) {
                    Text("Dismiss", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- SUB TABS FOR HOME (FEED & APPLICATIONS) ---

@Composable
fun LabourHomeFeedTab(onJobClick: (String) -> Unit) {
    var subTabState by remember { mutableStateOf(0) } // 0 = Feed, 1 = Applications
    
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTabState) {
            Tab(
                selected = subTabState == 0,
                onClick = { subTabState = 0 },
                text = { Text("Nearby Jobs", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                modifier = Modifier.height(48.dp)
            )
            Tab(
                selected = subTabState == 1,
                onClick = { subTabState = 1 },
                text = { Text("My Applications", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                modifier = Modifier.height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subTabState == 0) {
            if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No jobs available nearby.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(jobs) { job ->
                        JobFeedCard(
                            job = job, 
                            onClick = { onJobClick(job.id) },
                            onApplyClick = { /* Already logged in */ }
                        )
                    }
                }
            }
        } else {
            if (applications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You haven't applied to any jobs yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(applications) { app ->
                        ApplicationStatusCard(app = app, onJobClick = onJobClick)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationStatusCard(app: JobApplication, onJobClick: (String) -> Unit) {
    val (statusText, badgeColor) = when (app.status) {
        ApplicationStatus.APPLIED -> "Applied (ओटीपी भेजा)" to Color(0xFFFBC02D) // Yellow
        ApplicationStatus.ACCEPTED -> "Hired / Approved (काम मिला)" to Color(0xFF388E3C) // Green
        ApplicationStatus.COMPLETED -> "Completed (पूर्ण हुआ)" to Color(0xFF1976D2) // Blue
        ApplicationStatus.REJECTED -> "Rejected (अस्वीकृत)" to Color(0xFFD32F2F) // Red
    }

    Card(
        onClick = { onJobClick(app.jobId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(app.jobTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(statusText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Work, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Skills: ${app.labourSkills.joinToString(", ")}", fontSize = 12.sp, color = Color.DarkGray)
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
            placeholder = { Text("Search by trade, area, or keywords...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal filter chips
        Text("Quick Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
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
                shape = RoundedCornerShape(8.dp)
            )

            FilterChip(
                selected = selectedDistance != null,
                onClick = {
                    selectedDistance = if (selectedDistance == null) 5.0 else null
                },
                label = { Text("Under 5 km") },
                shape = RoundedCornerShape(8.dp)
            )

            FilterChip(
                selected = selectedWage != null,
                onClick = {
                    selectedWage = if (selectedWage == null) 700 else null
                },
                label = { Text("₹700+ / Day") },
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredJobs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No matching jobs found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

    if (threads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active job chats yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(threads) { thread ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatThreadClick(thread.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFE0F7FA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thread.otherUserName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF006064)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(thread.otherUserName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (thread.otherUserVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 14)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(thread.lastMessageText, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (thread.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(thread.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

// --- PROFILE & SETTINGS TAB ---

@Composable
fun LabourProfileTab(onLogoutClick: () -> Unit) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    var isUploadingDoc by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            // Profile Card Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser?.name ?: "Worker", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (currentUser?.isVerified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedBadge(size = 18)
                        }
                    }
                    Text(currentUser?.phone ?: "", color = Color.Gray, fontSize = 14.sp)
                    Text("Role: Daily Wage Worker (Labour)", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Divider()
        }

        item {
            // Availability Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("My Availability Today", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (currentUser?.labourProfile?.isAvailable == true) "Available for job invitations" else "Busy / Working",
                            color = if (currentUser?.labourProfile?.isAvailable == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 13.sp
                        )
                    }
                    Switch(
                        checked = currentUser?.labourProfile?.isAvailable == true,
                        onCheckedChange = { RozgarRepository.toggleAvailability(it) }
                    )
                }
            }
        }

        item {
            // Aadhaar Verification Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trust Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Upload government ID to get a Verified Badge. Verified badge doubles response chance.", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (currentUser?.isVerified == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Done, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ID Verified & Badge Granted!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                isUploadingDoc = true
                                uploadStatus = "Uploading Aadhaar..."
                                val flow = RozgarRepository.uploadIdDocument("Aadhaar")
                                scope.launch {
                                    flow.collectLatest { res ->
                                        isUploadingDoc = false
                                        res.fold(
                                            onSuccess = {
                                                uploadStatus = "Upload successful! Verified Badge Granted!"
                                                RozgarRepository.addNotification("Verification Approved", "Your account has been verified successfully.")
                                            },
                                            onFailure = { error ->
                                                uploadStatus = "Error: " + error.localizedMessage
                                            }
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isUploadingDoc
                        ) {
                            if (isUploadingDoc) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verify Aadhaar (ID Proof)")
                                }
                            }
                        }
                        uploadStatus?.let {
                            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        item {
            // Offline Mode Simulator Toggle (For testing NFR scenarios)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                border = BorderStroke(1.dp, Color(0xFFFBC02D))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Offline Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("Test how the application behaves when connection is lost.", fontSize = 11.sp, color = Color.DarkGray)
                    }
                    Switch(
                        checked = isOffline,
                        onCheckedChange = { RozgarRepository.toggleOffline(it) }
                    )
                }
            }
        }

        item {
            PrimaryLargeButton(
                text = "Logout Account",
                onClick = {
                    RozgarRepository.logout()
                    onLogoutClick()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            )
        }
    }
}
