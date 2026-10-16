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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.*
import com.rozgarmitra.app.presentation.components.*
import com.rozgarmitra.app.presentation.worker.tradesList
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.presentation.common.getJobStatusDisplayName
import com.rozgarmitra.app.presentation.common.getTradeDisplayName
import com.rozgarmitra.app.presentation.common.getWageTypeDisplayName
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHomeScreen(
    onJobClick: (String) -> Unit,
    onManageApplicantsClick: (String) -> Unit,
    onChatThreadClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onHelpCenterClick: () -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit,
    onActiveJobsClick: () -> Unit = {},
    onApplicationsClick: () -> Unit = {},
    onWorkersHiredClick: () -> Unit = {}
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
                                color = AccentCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    stringResource(R.string.employer_badge), 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = AccentCyan,
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
                    label = stringResource(R.string.nav_post_job),
                    icon = Icons.Filled.AddCircle,
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
                0 -> OwnerDashboardTab(
                    onJobClick = onJobClick, 
                    onManageApplicantsClick = onManageApplicantsClick,
                    onNavigateToPost = { selectedTab = 1 },
                    onCompleteProfileClick = onCompleteProfileClick,
                    onActiveJobsClick = onActiveJobsClick,
                    onApplicationsClick = onApplicationsClick,
                    onWorkersHiredClick = onWorkersHiredClick
                )
                1 -> OwnerPostJobTab(onPostSuccess = { selectedTab = 0 })
                2 -> OwnerChatsTab(onChatThreadClick = onChatThreadClick)
                3 -> OwnerProfileTab(
                    onSettingsClick = onSettingsClick,
                    onHelpCenterClick = onHelpCenterClick,
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
                        com.rozgarmitra.app.data.NotificationType.JOB_APPLICATION.name -> {
                            if (notif.relatedJobId.isNotBlank()) {
                                onManageApplicantsClick(notif.relatedJobId)
                            } else {
                                onApplicationsClick()
                            }
                        }
                        com.rozgarmitra.app.data.NotificationType.NEW_MESSAGE.name -> {
                            if (notif.relatedThreadId.isNotBlank()) {
                                onChatThreadClick(notif.relatedThreadId)
                            } else {
                                selectedTab = 2
                            }
                        }
                        com.rozgarmitra.app.data.NotificationType.JOB_STATUS_CHANGED.name -> {
                            if (notif.relatedJobId.isNotBlank()) {
                                onJobClick(notif.relatedJobId)
                            } else {
                                onActiveJobsClick()
                            }
                        }
                        else -> {
                            if (notif.relatedJobId.isNotBlank()) {
                                onJobClick(notif.relatedJobId)
                            }
                        }
                    }
                    scope.launch {
                        val res = RozgarRepository.markNotificationRead(notif.id)
                        if (res.isFailure) {
                            android.util.Log.e("OwnerHomeScreen", "Failed to mark notification read in Firestore: ${notif.id}", res.exceptionOrNull())
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OwnerHomeScreen", "Failed to navigate for notification: ${notif.id}", e)
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
    onNavigateToPost: () -> Unit,
    onCompleteProfileClick: () -> Unit = {},
    onActiveJobsClick: () -> Unit = {},
    onApplicationsClick: () -> Unit = {},
    onWorkersHiredClick: () -> Unit = {}
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()

    val ownerJobs = jobs.filter { it.ownerId == currentUser?.id }
    val activeJobsCount = ownerJobs.count { it.status == JobStatus.ACTIVE }
    val completedJobsCount = ownerJobs.count { it.status == JobStatus.COMPLETED || it.status == JobStatus.FILLED }
    val totalWorkersHired = ownerJobs.sumOf { it.acceptedWorkersCount }

    val activeJobIds = ownerJobs.map { it.id }
    val ownerApps = applications.filter { it.jobId in activeJobIds || it.ownerId == currentUser?.id }
    val applicantsWaitingCount = ownerApps.count { it.status == ApplicationStatus.APPLIED }

    val ownerDisplayName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "Owner"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.greeting_namaste_user, ownerDisplayName), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(stringResource(R.string.owner_dashboard_subtitle), fontSize = 13.sp, color = TextSecondary)
            }
        }

        if (currentUser?.profileCompleted != true) {
            item {
                Surface(
                    color = WarningRose.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
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

        // Summary Metric Cards row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Active
                Card(
                    onClick = onActiveJobsClick,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.metric_active_jobs), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeJobsCount.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                    }
                }

                // Metric 2: Applications
                Card(
                    onClick = onApplicationsClick,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.metric_applications), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            if (applicantsWaitingCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(modifier = Modifier.size(6.dp).background(WarningRose, CircleShape))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ownerApps.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                    }
                }

                // Metric 3: Workers Hired
                Card(
                    onClick = onWorkersHiredClick,
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.metric_workers_hired), fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(totalWorkersHired.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    }
                }
            }
        }


        // Post a job button
        item {
            Card(
                onClick = onNavigateToPost,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, PrimaryIndigo),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_post_new_job_now), color = PrimaryIndigo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Recent Worker Responses Section
        if (ownerApps.isNotEmpty()) {
            item {
                Text(stringResource(R.string.recent_worker_responses), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(top = 4.dp))
            }

            items(ownerApps.take(3)) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.labourName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(stringResource(R.string.status_applied) + ": ${app.jobTitle}", fontSize = 12.sp, color = TextSecondary)
                        }

                        Button(
                            onClick = { onManageApplicantsClick(app.jobId) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(stringResource(R.string.tap_to_view_details).substringBefore(" "), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Text(stringResource(R.string.owner_dashboard_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
        }

        if (ownerJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_active_postings), color = TextSecondary)
                }
            }
        } else {
            items(ownerJobs) { job ->
                val jobApps = ownerApps.filter { it.jobId == job.id }
                val newAppsCount = jobApps.count { it.status == ApplicationStatus.APPLIED }

                val badgeText = getJobStatusDisplayName(job.status)
                val badgeColor = when (job.status) {
                    JobStatus.ACTIVE -> SuccessGreen
                    JobStatus.FILLED -> PrimaryIndigo
                    JobStatus.COMPLETED -> TextSecondary
                    JobStatus.CLOSED -> WarningRose
                    JobStatus.DRAFT -> Color(0xFFF59E0B)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${job.wage}/${getWageTypeDisplayName(job.wageType)} • 📍 ${job.location}", fontSize = 13.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("👷 " + stringResource(R.string.workers_needed_count, job.numberOfWorkersRequired, job.acceptedWorkersCount), fontSize = 12.sp, color = PrimaryIndigo, fontWeight = FontWeight.Medium)
                            }

                            Surface(
                                color = badgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = badgeText,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                Text(stringResource(R.string.job_details_title), color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onManageApplicantsClick(job.id) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.metric_applications) + " (${jobApps.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (newAppsCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = WarningRose,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
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
    val context = LocalContext.current

    // Form states
    var category by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var isLocatingLocation by remember { mutableStateOf(false) }
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
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Steps indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.step_indicator, step), fontWeight = FontWeight.Bold, color = PrimaryIndigo, fontSize = 14.sp)
                LinearProgressIndicator(
                    progress = step / 5.0f,
                    color = PrimaryIndigo,
                    trackColor = DarkSurfaceVariant,
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (step) {
                1 -> {
                    // Step 1: Select Category
                    Text(stringResource(R.string.step_1_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.height(350.dp)
                    ) {
                        val tradesWithOther = tradesList.map { it.substringBefore(" (") } + "Other"

                        items(tradesWithOther) { trade ->
                            val isSelected = category == trade
                            Card(
                                onClick = {
                                    if (trade == "Other") {
                                        isSuggestingOther = true
                                    } else {
                                        category = trade
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else DarkSurface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryIndigo else BorderStrokeColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(14.dp),
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
                                        trade == "Other" -> Icons.Filled.Add
                                        else -> Icons.Filled.Work
                                    }

                                    Icon(
                                        imageVector = tradeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = if (isSelected) PrimaryIndigo else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val tradeLabel = if (trade == "Other") stringResource(R.string.trade_other) else getTradeDisplayName(trade)
                                    Text(
                                        tradeLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = if (isSelected) PrimaryIndigo else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Step 2: Work Details
                    Text(stringResource(R.string.step_2_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.job_title_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text(stringResource(R.string.job_title_placeholder), color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.job_desc_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(stringResource(R.string.job_desc_placeholder), color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.work_location_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                        TextButton(
                            onClick = {
                                isLocatingLocation = true
                                scope.launch {
                                    val locData = LocationHelper.getCurrentLocation(context)
                                    isLocatingLocation = false
                                    if (locData != null) {
                                        location = locData.addressName
                                        latitude = locData.latitude
                                        longitude = locData.longitude
                                    }
                                }
                            },
                            enabled = !isLocatingLocation
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isLocatingLocation) stringResource(R.string.detecting_location) else stringResource(R.string.use_gps_location), color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text(stringResource(R.string.work_location_placeholder), color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Date & Time
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.start_date_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                placeholder = { Text(stringResource(R.string.today), color = TextMuted) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.start_time_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = { startTime = it },
                                placeholder = { Text("8:00 AM", color = TextMuted) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Workers required & duration
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.workers_needed_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (workersRequired > 1) workersRequired-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = TextPrimary)
                                }
                                Text(workersRequired.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { workersRequired++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = TextPrimary)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.duration_days_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (durationDays > 1) durationDays-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease", tint = TextPrimary)
                                }
                                Text(durationDays.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp))
                                IconButton(onClick = { durationDays++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase", tint = TextPrimary)
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Step 3: Payment
                    Text(stringResource(R.string.step_3_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.wage_amount_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = wage,
                            onValueChange = { if (it.all { char -> char.isDigit() }) wage = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        listOf("500", "700", "900").forEach { p ->
                            OutlinedButton(onClick = { wage = p }, shape = CircleShape, border = BorderStroke(1.dp, BorderStrokeColor)) {
                                Text("₹$p", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.wage_type_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("per day", "per hour", "fixed").forEach { type ->
                            val isSelected = wageType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { wageType = type },
                                label = { Text(getWageTypeDisplayName(type)) },
                                shape = CircleShape,
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = Color.White, containerColor = DarkSurface, labelColor = TextSecondary),
                                border = FilterChipDefaults.filterChipBorder(borderColor = BorderStrokeColor, selectedBorderColor = PrimaryIndigo)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(stringResource(R.string.included_perks_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = perkFood,
                            onClick = { perkFood = !perkFood },
                            label = { Text(stringResource(R.string.perk_food)) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = Color.White, containerColor = DarkSurface, labelColor = TextSecondary),
                            border = FilterChipDefaults.filterChipBorder(borderColor = BorderStrokeColor, selectedBorderColor = PrimaryIndigo)
                        )
                        FilterChip(
                            selected = perkAccommodation,
                            onClick = { perkAccommodation = !perkAccommodation },
                            label = { Text(stringResource(R.string.perk_shelter)) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = Color.White, containerColor = DarkSurface, labelColor = TextSecondary),
                            border = FilterChipDefaults.filterChipBorder(borderColor = BorderStrokeColor, selectedBorderColor = PrimaryIndigo)
                        )
                        FilterChip(
                            selected = perkTransport,
                            onClick = { perkTransport = !perkTransport },
                            label = { Text(stringResource(R.string.perk_transport)) },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryIndigo, selectedLabelColor = Color.White, containerColor = DarkSurface, labelColor = TextSecondary),
                            border = FilterChipDefaults.filterChipBorder(borderColor = BorderStrokeColor, selectedBorderColor = PrimaryIndigo)
                        )
                    }
                }
                4 -> {
                    // Step 4: Requirements & Urgent Flag
                    Text(stringResource(R.string.step_4_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.skills_required_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = skillsRequired,
                        onValueChange = { skillsRequired = it },
                        placeholder = { Text(stringResource(R.string.skills_required_placeholder), color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkSurface, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(stringResource(R.string.difficulty_level_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Easy", "Medium", "Hard").forEach { lvl ->
                            val isSelected = difficulty == lvl
                            val lvlLabel = when (lvl) {
                                "Easy" -> stringResource(R.string.difficulty_easy)
                                "Medium" -> stringResource(R.string.difficulty_medium)
                                else -> stringResource(R.string.difficulty_hard)
                            }
                            OutlinedButton(
                                onClick = { difficulty = lvl },
                                modifier = Modifier.weight(1f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryIndigo else BorderStrokeColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text(lvlLabel, color = if (isSelected) PrimaryIndigo else TextSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isUrgent,
                            onCheckedChange = { isUrgent = it },
                            colors = CheckboxDefaults.colors(checkedColor = WarningRose, checkmarkColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(stringResource(R.string.mark_urgent_hiring), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(stringResource(R.string.urgent_hiring_desc), fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                5 -> {
                    // Step 5: Review & Publish
                    Text(stringResource(R.string.step_5_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, BorderStrokeColor)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = PrimaryIndigo.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Work, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(title.ifBlank { "Untitled Job" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                    Text(getTradeDisplayName(category), fontSize = 13.sp, color = PrimaryIndigo, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 14.dp), color = BorderStrokeColor.copy(alpha = 0.6f))

                            PreviewRow(Icons.Filled.LocationOn, stringResource(R.string.preview_location), location)
                            PreviewRow(Icons.Filled.CalendarMonth, stringResource(R.string.preview_date_time), "$date • $startTime")
                            PreviewRow(Icons.Filled.Group, stringResource(R.string.workers_needed_label), "$workersRequired")
                            PreviewRow(Icons.Filled.Payments, stringResource(R.string.preview_wage), "₹$wage / " + getWageTypeDisplayName(wageType), valueColor = SuccessGreen)

                            val perksList = mutableListOf<String>()
                            if (perkFood) perksList.add(stringResource(R.string.perk_food))
                            if (perkAccommodation) perksList.add(stringResource(R.string.perk_shelter))
                            if (perkTransport) perksList.add(stringResource(R.string.perk_transport))
                            PreviewRow(Icons.Filled.Star, stringResource(R.string.preview_perks), perksList.joinToString(", ").ifBlank { "None" })

                            if (isUrgent) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(color = WarningRose.copy(alpha = 0.15f), border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.3f)), shape = CircleShape) {
                                    Text(stringResource(R.string.urgent_hiring_active), color = WarningRose, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
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
                Text(text = postError ?: "", color = WarningRose, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, BorderStrokeColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(stringResource(R.string.btn_previous), color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else {
                            scope.launch {
                                isLoading = true
                                postError = null
                                var jobLat = latitude
                                var jobLng = longitude
                                if (jobLat == 0.0 && jobLng == 0.0 && location.isNotBlank()) {
                                    val coords = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        LocationHelper.getCoordinatesFromAddress(context, location.trim())
                                    }
                                    if (coords != null) {
                                        jobLat = coords.first
                                        jobLng = coords.second
                                    }
                                }
                                val perks = mutableListOf<String>()
                                if (perkFood) perks.add("Food Provided")
                                if (perkAccommodation) perks.add("Accommodation Provided")
                                if (perkTransport) perks.add("Transport Provided")

                                RozgarRepository.postJob(
                                    title = title,
                                    category = category,
                                    description = description,
                                    location = location,
                                    latitude = jobLat,
                                    longitude = jobLng,
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
                                ).collect { res ->
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp),
                    enabled = when (step) {
                        1 -> category.isNotBlank()
                        2 -> title.isNotBlank() && location.isNotBlank()
                        3 -> wage.isNotBlank()
                        else -> true
                    }
                ) {
                    Text(if (step == 5) stringResource(R.string.btn_publish_job) else stringResource(R.string.btn_next), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    com.rozgarmitra.app.presentation.components.LoadingOverlay(isLoading = isLoading, text = stringResource(R.string.publishing_job))

    if (isSuggestingOther) {
        AlertDialog(
            onDismissRequest = { isSuggestingOther = false },
            containerColor = DarkSurface,
            title = { Text("Suggest New Category", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Can't find your trade? Suggest it to us.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = suggestedCategoryName,
                        onValueChange = { suggestedCategoryName = it },
                        label = { Text("Trade Name (e.g. Driver)", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkBackground, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suggestedCategoryDesc,
                        onValueChange = { suggestedCategoryDesc = it },
                        label = { Text("Brief Description about work", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = DarkBackground, focusedBorderColor = PrimaryIndigo, unfocusedBorderColor = BorderStrokeColor, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    enabled = suggestedCategoryName.isNotBlank() && suggestedCategoryDesc.isNotBlank() && !suggestionLoading
                ) {
                    if (suggestionLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Submit Suggestion", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isSuggestingOther = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
fun PreviewRow(icon: ImageVector, label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "$label: ", fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

// --- TAB 3: OWNER CHATS TAB ---

@Composable
fun OwnerChatsTab(onChatThreadClick: (String) -> Unit) {
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

// --- TAB 4: OWNER PROFILE TAB ---

@Composable
fun OwnerProfileTab(
    onSettingsClick: () -> Unit,
    onHelpCenterClick: () -> Unit = {},
    onCompleteProfileClick: () -> Unit = {},
    onLogoutClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showBusinessInfoDialog by remember { mutableStateOf(false) }
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
                        Text(currentUser?.name ?: "Employer", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
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
                        color = AccentCyan.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f)),
                        shape = CircleShape,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            stringResource(R.string.employer_badge), 
                            color = AccentCyan, 
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
            ProfileSection(title = stringResource(R.string.business_details)) {
                ProfileMenuItem(
                    icon = Icons.Filled.Business,
                    title = "My Company",
                    subtitle = currentUser?.ownerProfile?.companyName?.ifBlank { "Tap to specify company" } ?: "Tap to specify company",
                    onClick = { showBusinessInfoDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.LocationOn,
                    title = "Work Site Address",
                    subtitle = currentUser?.ownerProfile?.address?.ifBlank { "Tap to specify address" } ?: "Tap to specify address",
                    onClick = { showBusinessInfoDialog = true }
                )
            }
        }

        item {
            val isVer = currentUser?.isVerified == true
            val isPen = currentUser?.verificationStatus == "PENDING"
            ProfileSection(title = "Verification") {
                ProfileMenuItem(
                    icon = Icons.Filled.VerifiedUser,
                    title = when {
                        isVer -> "Business Verified"
                        isPen -> stringResource(R.string.verification_pending)
                        else -> "Verify Business"
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
                    onClick = onHelpCenterClick
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

    if (showBusinessInfoDialog) {
        var companyInput by remember { mutableStateOf(currentUser?.ownerProfile?.companyName ?: "") }
        var addressInput by remember { mutableStateOf(currentUser?.ownerProfile?.address ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { if (!isSaving) showBusinessInfoDialog = false },
            title = { Text(stringResource(R.string.business_details), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = companyInput,
                        onValueChange = { companyInput = it },
                        label = { Text("Company / Organization Name", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStrokeColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text(stringResource(R.string.work_location_label), color = TextSecondary) },
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
                        isSaving = true
                        scope.launch {
                            RozgarRepository.updateOwnerBusinessInfo(companyInput.trim(), addressInput.trim()).collectLatest {
                                isSaving = false
                                showBusinessInfoDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(if (isSaving) stringResource(R.string.action_loading) else stringResource(R.string.action_save), color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBusinessInfoDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }

    if (showVerificationDialog) {
        var selectedDocType by remember { mutableStateOf("GSTIN Registration") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { if (!isSubmitting) showVerificationDialog = false },
            title = { Text(stringResource(R.string.verify_aadhaar), color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.submit_id_verification), fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("GSTIN Registration", "Trade License", "MSME Registration", "Owner Aadhaar").forEach { doc ->
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

