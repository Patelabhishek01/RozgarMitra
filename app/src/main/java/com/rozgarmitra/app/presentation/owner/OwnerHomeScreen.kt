package com.rozgarmitra.app.presentation.owner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.*
import com.rozgarmitra.app.presentation.components.OfflineBanner
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.ProfileMenuItem
import com.rozgarmitra.app.presentation.components.ProfileSection
import com.rozgarmitra.app.presentation.components.VerifiedBadge
import com.rozgarmitra.app.presentation.worker.tradesList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    onJobClick: (String) -> Unit,
    onManageApplicantsClick: (String) -> Unit,
    onChatThreadClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RozgarMitra", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "EMPLOYER / OWNER", 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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
                modifier = Modifier.height(80.dp)
            ) {
                val items = listOf(
                    Triple("Home", Icons.Filled.Home, 0),
                    Triple("Post Job", Icons.Filled.AddCircle, 1),
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
                0 -> OwnerDashboardTab(
                    onJobClick = onJobClick, 
                    onManageApplicantsClick = onManageApplicantsClick,
                    onNavigateToPost = { selectedTab = 1 }
                )
                1 -> OwnerPostJobTab(onPostSuccess = { selectedTab = 0 })
                2 -> OwnerChatsTab(onChatThreadClick = onChatThreadClick)
                3 -> OwnerProfileTab(
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick
                )
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

// --- TAB 1: OWNER DASHBOARD ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardTab(
    onJobClick: (String) -> Unit,
    onManageApplicantsClick: (String) -> Unit,
    onNavigateToPost: () -> Unit
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()

    // Filter jobs posted by this owner
    val ownerJobs = jobs.filter { it.ownerId == currentUser?.id }
    val activeJobsCount = ownerJobs.count { it.status == JobStatus.ACTIVE }
    val completedJobsCount = ownerJobs.count { it.status == JobStatus.COMPLETED || it.status == JobStatus.FILLED }
    val totalWorkersHired = ownerJobs.sumOf { it.acceptedWorkersCount }

    // Count applicants waiting
    val activeJobIds = ownerJobs.map { it.id }
    val ownerApps = applications.filter { it.jobId in activeJobIds || it.ownerId == currentUser?.id }
    val applicantsWaitingCount = ownerApps.count { it.status == ApplicationStatus.APPLIED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Welcome, ${currentUser?.name ?: "Employer"} 👋", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("Manage your job postings and applicants", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // Summary Metric Cards row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Active
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Jobs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(activeJobsCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Metric 2: Applications
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Applications", fontSize = 11.sp, color = Color(0xFFE65100))
                            if (applicantsWaitingCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                            }
                        }
                        Text(ownerApps.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                    }
                }

                // Metric 3: Workers Hired
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Workers Hired", fontSize = 11.sp, color = Color(0xFF1B5E20))
                        Text(totalWorkersHired.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                    }
                }
            }
        }

        // Post a job button
        item {
            Card(
                onClick = onNavigateToPost,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ Post a New Job Now", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Recent Worker Responses Section
        if (ownerApps.isNotEmpty()) {
            item {
                Text("Recent Worker Responses", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            }

            items(ownerApps.take(3)) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.labourName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Responded to: ${app.jobTitle}", fontSize = 12.sp, color = Color.Gray)
                        }

                        Button(
                            onClick = { onManageApplicantsClick(app.jobId) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View Responses", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("My Posted Jobs", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }

        if (ownerJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No job posts yet. Click above to create one.", color = Color.Gray)
                }
            }
        } else {
            items(ownerJobs) { job ->
                val jobApps = ownerApps.filter { it.jobId == job.id }
                val newAppsCount = jobApps.count { it.status == ApplicationStatus.APPLIED }

                val (badgeText, badgeColor) = when (job.status) {
                    JobStatus.ACTIVE -> "ACTIVE" to Color(0xFF2E7D32)
                    JobStatus.FILLED -> "FILLED" to Color(0xFF1565C0)
                    JobStatus.COMPLETED -> "COMPLETED" to Color(0xFF37474F)
                    JobStatus.CLOSED -> "CLOSED" to Color(0xFFC62828)
                    JobStatus.DRAFT -> "DRAFT" to Color(0xFFE65100)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${job.wage}/${job.wageType} • 📍 ${job.location}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("👷 Workers: ${job.acceptedWorkersCount} / ${job.numberOfWorkersRequired} hired", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }

                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onJobClick(job.id) }) {
                                Text("View Details")
                            }

                            Button(
                                onClick = { onManageApplicantsClick(job.id) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Applications (${jobApps.size})")
                                    if (newAppsCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(Color.Red, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(newAppsCount.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: OWNER POST JOB (STEP-BY-STEP FORM) ---

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OwnerPostJobTab(
    onPostSuccess: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // Steps 1 to 5
    var isLoading by remember { mutableStateOf(false) }
    var postError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Form states
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Vijay Nagar, Indore") }
    var date by remember { mutableStateOf("Today") }
    var startTime by remember { mutableStateOf("8:00 AM") }
    var durationDays by remember { mutableStateOf(1) }
    var hoursPerDay by remember { mutableStateOf(8) }
    var workersRequired by remember { mutableStateOf(1) }
    var wage by remember { mutableStateOf("700") }
    var wageType by remember { mutableStateOf("per day") } // per day, per hour, fixed

    // Perks list
    var perkFood by remember { mutableStateOf(false) }
    var perkAccommodation by remember { mutableStateOf(false) }
    var perkTransport by remember { mutableStateOf(false) }

    var difficulty by remember { mutableStateOf("Medium") }
    var skillsRequired by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

    // Suggestion states
    var isSuggestingOther by remember { mutableStateOf(false) }
    var suggestedCategoryName by remember { mutableStateOf("") }
    var suggestedCategoryDesc by remember { mutableStateOf("") }
    var suggestionLoading by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Steps indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Step $step of 5", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                LinearProgressIndicator(
                    progress = step / 5.0f,
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (step) {
                1 -> {
                    // Step 1: Select Category
                    Text("1. Basic Information / कार्य की श्रेणी", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(350.dp)
                    ) {
                        val tradesWithOther = tradesList.map { it.substringBefore(" (") } + "Other (अन्य)"

                        items(tradesWithOther) { trade ->
                            val isSelected = category == trade
                            Card(
                                onClick = {
                                    if (trade == "Other (अन्य)") {
                                        isSuggestingOther = true
                                    } else {
                                        category = trade
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else if (trade == "Other (अन्य)") Color(0xFFFFF3E0)
                                    else Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F0)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val tradeIcon = when {
                                        trade.contains("Mason") -> Icons.Filled.Foundation
                                        trade.contains("Painter") -> Icons.Filled.Brush
                                        trade.contains("Electrician") -> Icons.Filled.Bolt
                                        trade.contains("Plumber") -> Icons.Filled.WaterDrop
                                        trade.contains("Driver") -> Icons.Filled.DirectionsCar
                                        trade.contains("Cleaner") -> Icons.Filled.CleaningServices
                                        trade.contains("Construction") -> Icons.Filled.Build
                                        trade == "Other (अन्य)" -> Icons.Filled.Add
                                        else -> Icons.Filled.Work
                                    }

                                    Icon(
                                        imageVector = tradeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        trade,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Step 2: Work Details
                    Text("2. Work Details / स्थान और समय", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Job Title", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Need 3 construction helpers") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Job Description", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Describe the work responsibilities...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Work Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text("e.g. Vijay Nagar, Indore") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date & Time
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Date", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                placeholder = { Text("e.g. 25 Sept / Today") },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Start Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                placeholder = { Text("e.g. 8:00 AM") },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Workers required & duration
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Workers Needed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (workersRequired > 1) workersRequired-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(workersRequired.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { workersRequired++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration (Days)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (durationDays > 1) durationDays-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(durationDays.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { durationDays++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Step 3: Payment
                    Text("3. Payment & Perks / मजदूरी", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Wage Amount (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = wage,
                            onValueChange = { if (it.all { char -> char.isDigit() }) wage = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf("500", "700", "900").forEach { p ->
                            OutlinedButton(onClick = { wage = p }, shape = RoundedCornerShape(8.dp)) {
                                Text("₹$p")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Wage Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("per day", "per hour", "fixed").forEach { type ->
                            val isSelected = wageType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { wageType = type },
                                label = { Text(type.replaceFirstChar { it.uppercase() }) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Included Perks (Optional)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = perkFood,
                            onClick = { perkFood = !perkFood },
                            label = { Text("Food") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = perkAccommodation,
                            onClick = { perkAccommodation = !perkAccommodation },
                            label = { Text("Shelter") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = perkTransport,
                            onClick = { perkTransport = !perkTransport },
                            label = { Text("Transport") },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                4 -> {
                    // Step 4: Requirements & Urgent Flag
                    Text("4. Skills & Requirements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Skills Required (Comma separated)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = skillsRequired,
                        onValueChange = { skillsRequired = it },
                        placeholder = { Text("e.g. bricklaying, cement mixing, loading") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Difficulty Level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { lvl ->
                            val isSelected = difficulty == lvl
                            OutlinedButton(
                                onClick = { difficulty = lvl },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                )
                            ) {
                                Text(lvl, color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Mark as Urgent Hiring", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Urgent jobs are highlighted red to workers", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                5 -> {
                    // Step 5: Review & Publish
                    Text("5. Review & Publish Job", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(title.ifBlank { "Untitled Job" }, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(category, fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF5F5F5))

                            PreviewRow(Icons.Filled.LocationOn, "Location", location)
                            PreviewRow(Icons.Filled.CalendarMonth, "Date & Time", "$date • $startTime")
                            PreviewRow(Icons.Filled.Group, "Workers Needed", "$workersRequired workers required")
                            PreviewRow(Icons.Filled.Payments, "Wage", "₹$wage / $wageType", valueColor = Color(0xFF2E7D32))

                            val perksList = mutableListOf<String>()
                            if (perkFood) perksList.add("Food")
                            if (perkAccommodation) perksList.add("Shelter")
                            if (perkTransport) perksList.add("Transport")
                            PreviewRow(Icons.Filled.Star, "Perks", perksList.joinToString(", ").ifBlank { "None" })

                            if (isUrgent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                                    Text("⚠️ URGENT HIRING ACTIVE", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(visible = postError != null) {
                Text(text = postError ?: "", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else {
                            // Publish job flow
                            isLoading = true
                            postError = null
                            val perks = mutableListOf<String>()
                            if (perkFood) perks.add("Food Provided")
                            if (perkAccommodation) perks.add("Accommodation Provided")
                            if (perkTransport) perks.add("Transport Provided")

                            val flow = RozgarRepository.postJob(
                                title = title,
                                category = category,
                                description = description,
                                location = location,
                                date = date,
                                startTime = startTime,
                                duration = "$durationDays Days",
                                numberOfWorkersRequired = workersRequired,
                                wage = wage.toIntOrNull() ?: 0,
                                wageType = wageType,
                                days = durationDays,
                                hours = hoursPerDay,
                                perks = perks,
                                difficulty = difficulty,
                                skills = skillsRequired.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                urgent = isUrgent
                            )
                            scope.launch {
                                flow.collect { res ->
                                    isLoading = false
                                    res.fold(
                                        onSuccess = {
                                            onPostSuccess()
                                        },
                                        onFailure = { err ->
                                            postError = err.localizedMessage
                                        }
                                    )
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    enabled = when (step) {
                        1 -> category.isNotBlank()
                        2 -> title.isNotBlank() && location.isNotBlank()
                        3 -> wage.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(if (step == 5) "Publish Job" else "Next Step")
                }
            }
        }
    }

    
    com.rozgarmitra.app.presentation.components.LoadingOverlay(isLoading = isLoading, text = "Publishing Job...")

    if (isSuggestingOther) {
        AlertDialog(
            onDismissRequest = { isSuggestingOther = false },
            title = { Text("Suggest New Category", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Can't find your trade? Suggest it to us.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = suggestedCategoryName,
                        onValueChange = { suggestedCategoryName = it },
                        label = { Text("Trade Name (e.g. Driver)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suggestedCategoryDesc,
                        onValueChange = { suggestedCategoryDesc = it },
                        label = { Text("Brief Description about work") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        suggestionLoading = true
                        scope.launch {
                            RozgarRepository.suggestNewCategory(suggestedCategoryName, suggestedCategoryDesc).collect { result ->
                                suggestionLoading = false
                                result.onSuccess {
                                    isSuggestingOther = false
                                    postError = "Suggestion submitted! We will review and add it soon."
                                }
                            }
                        }
                    },
                    enabled = suggestedCategoryName.isNotBlank() && suggestedCategoryDesc.isNotBlank() && !suggestionLoading
                ) {
                    if (suggestionLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Submit Suggestion")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSuggestingOther = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PreviewRow(icon: ImageVector, label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "$label: ", fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// --- TAB 3: OWNER CHATS TAB ---

@Composable
fun OwnerChatsTab(onChatThreadClick: (String) -> Unit) {
    val threads by RozgarRepository.threads.collectAsStateWithLifecycle()

    if (threads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active chats with workers yet.", color = Color.Gray)
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
                            .background(Color(0xFFE8F5E9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thread.otherUserName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1B5E20)
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

// --- TAB 4: OWNER PROFILE TAB ---

@Composable
fun OwnerProfileTab(
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    
    var isUploadingDoc by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            // Profile Header Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
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
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(currentUser?.name ?: "Employer", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(currentUser?.phone ?: currentUser?.id ?: "", color = Color.Gray, fontSize = 14.sp)
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            "JOB POSTER / OWNER", 
                            color = MaterialTheme.colorScheme.primary, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            ProfileSection(title = "Business Information") {
                ProfileMenuItem(
                    icon = Icons.Filled.Business,
                    title = "My Company",
                    subtitle = currentUser?.ownerProfile?.companyName?.ifBlank { "Not Specified" } ?: "Not Specified",
                    onClick = { /* TODO */ }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.LocationOn,
                    title = "Work Site Address",
                    subtitle = currentUser?.ownerProfile?.address ?: "Not Specified",
                    onClick = { /* TODO */ }
                )
            }
        }

        item {
            ProfileSection(title = "Verification") {
                ProfileMenuItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = if (currentUser?.isVerified == true) "Business Verified" else "Verify Business",
                    subtitle = if (currentUser?.isVerified == true) "Badge active" else "Submit trade license",
                    titleColor = if (currentUser?.isVerified == true) Color(0xFF2E7D32) else Color.Unspecified,
                    onClick = { /* TODO */ }
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
                    title = "Help Center",
                    onClick = { /* TODO */ }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.BugReport,
                    title = "Debug: Offline Mode",
                    subtitle = "Test app without internet",
                    onClick = { RozgarRepository.toggleOffline(!isOffline) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            ProfileMenuItem(
                icon = Icons.Filled.Logout,
                title = "Logout Account",
                titleColor = Color.Red,
                onClick = {
                    RozgarRepository.logout()
                    onLogoutClick()
                }
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
