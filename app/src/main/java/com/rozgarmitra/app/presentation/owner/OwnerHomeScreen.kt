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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.*
import com.rozgarmitra.app.presentation.components.OfflineBanner
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
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
                3 -> OwnerProfileTab(onLogoutClick = onLogoutClick)
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
    val completedJobsCount = ownerJobs.count { it.status == JobStatus.COMPLETED }
    
    // Count applicants waiting
    val activeJobIds = ownerJobs.filter { it.status == JobStatus.ACTIVE }.map { it.id }
    val applicantsWaitingCount = applications.count { it.jobId in activeJobIds && it.status == ApplicationStatus.APPLIED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Business Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
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

                // Metric 2: Applicants
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Applicants", fontSize = 11.sp, color = Color(0xFFE65100))
                            if (applicantsWaitingCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                            }
                        }
                        Text(applicantsWaitingCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                    }
                }

                // Metric 3: Completed
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Completed", fontSize = 11.sp, color = Color(0xFF1B5E20))
                        Text(completedJobsCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                    }
                }
            }
        }

        // Post a job front & center target button
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
                    Text("Post a New Job Now", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        item {
            Text("My Posted Jobs", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp))
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
                val jobApps = applications.filter { it.jobId == job.id }
                val newAppsCount = jobApps.count { it.status == ApplicationStatus.APPLIED }

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
                            Column {
                                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Daily Wage: ₹${job.wage} • ${job.location}", fontSize = 12.sp, color = Color.Gray)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (job.status == JobStatus.ACTIVE) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (job.status == JobStatus.ACTIVE) "ACTIVE" else "COMPLETED",
                                    color = if (job.status == JobStatus.ACTIVE) Color(0xFF2E7D32) else Color(0xFF37474F),
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

                            if (job.status == JobStatus.ACTIVE) {
                                Button(
                                    onClick = { onManageApplicantsClick(job.id) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Applicants (${jobApps.size})")
                                        if (newAppsCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
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
    var location by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf(1) }
    var hoursPerDay by remember { mutableStateOf(8) }
    var wage by remember { mutableStateOf("600") }
    
    // Perks list
    var perkFood by remember { mutableStateOf(false) }
    var perkAccommodation by remember { mutableStateOf(false) }
    var perkTransport by remember { mutableStateOf(false) }
    
    var difficulty by remember { mutableStateOf("Medium") }
    var skillsRequired by remember { mutableStateOf("") }
    var isUrgent by remember { mutableStateOf(false) }

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
                    Text("Select Trade Category / श्रेणी चुनें", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(280.dp)
                    ) {
                        items(tradesList.map { it.substringBefore(" (") }) { trade ->
                            val isSelected = category == trade
                            Card(
                                onClick = { category = trade },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text(trade, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Step 2: Job details
                    Text("Job Details / कार्य का विवरण", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Job Title", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Need 2 Painters for room painting") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Work Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text("e.g. HSR Layout Sector 2") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Increments
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration (Days)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (durationDays > 1) durationDays-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(durationDays.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { durationDays++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Work Hours/Day", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (hoursPerDay > 1) hoursPerDay-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                                }
                                Text(hoursPerDay.toString(), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { if (hoursPerDay < 24) hoursPerDay++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Step 3: Wages & Perks
                    Text("Wages & Perks / मजदूरी और भत्ते", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Daily Wage (₹)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    // Step 4: Skills & Difficulty
                    Text("Skills & Requirements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Skills Required (Comma separated)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = skillsRequired,
                        onValueChange = { skillsRequired = it },
                        placeholder = { Text("e.g. wall scraping, double coat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Difficulty level", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    Text("Review & Publish Job", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(title.ifBlank { "Untitled Job" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Category: $category", fontSize = 13.sp, color = Color.Gray)
                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                            
                            Text("Location: $location", fontSize = 14.sp)
                            Text("Duration: $durationDays days • $hoursPerDay hours/day", fontSize = 14.sp)
                            Text("Daily Wage: ₹$wage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            
                            val perksList = mutableListOf<String>()
                            if (perkFood) perksList.add("Food")
                            if (perkAccommodation) perksList.add("Accommodation")
                            if (perkTransport) perksList.add("Transport")
                            Text("Perks: ${perksList.joinToString(", ").ifBlank { "None" }}", fontSize = 14.sp)
                            Text("Difficulty: $difficulty", fontSize = 14.sp)
                            Text("Required Skills: $skillsRequired", fontSize = 14.sp)
                            if (isUrgent) {
                                Text("⚠️ Urgent Hiring status active", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                location = location,
                                wage = wage.toIntOrNull() ?: 0,
                                durationDays = durationDays,
                                hoursPerDay = hoursPerDay,
                                perks = perks,
                                difficulty = difficulty,
                                skillsRequired = skillsRequired.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                isUrgent = isUrgent
                            )
                            scope.launch {
                                flow.collectLatest { res ->
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
fun OwnerProfileTab(onLogoutClick: () -> Unit) {
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
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Business, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser?.name ?: "Employer", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (currentUser?.isVerified == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            VerifiedBadge(size = 18)
                        }
                    }
                    Text(currentUser?.phone ?: "", color = Color.Gray, fontSize = 14.sp)
                    Text("Role: Job Poster / Owner", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Divider()
        }

        item {
            // Address & Business Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Business Information", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Company: ${currentUser?.ownerProfile?.companyName?.ifBlank { "Not Specified" }}", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Site Address: ${currentUser?.ownerProfile?.address}", fontSize = 13.sp, color = Color.DarkGray)
                }
            }
        }

        item {
            // Verification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Business Verification", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Upload business registration or trade license for a Verified Business Badge.", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (currentUser?.isVerified == true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Done, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Business Verified & Badge Granted!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                isUploadingDoc = true
                                uploadStatus = "Uploading Document..."
                                val flow = RozgarRepository.uploadIdDocument("Trade License")
                                scope.launch {
                                    flow.collectLatest { res ->
                                        isUploadingDoc = false
                                        res.fold(
                                            onSuccess = {
                                                uploadStatus = "Upload successful! Business Verified!"
                                                RozgarRepository.addNotification("Verification Approved", "Your business verification has been approved.")
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
                                    Text("Upload Trade License (PDF/JPG)")
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
            // Offline Mode Simulator Toggle
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
