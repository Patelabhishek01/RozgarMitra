package com.rozgarmitra.app.presentation.owner

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.ApplicationStatus
import com.rozgarmitra.app.data.JobApplication
import com.rozgarmitra.app.data.JobStatus
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.launch

// --- 1. MY ACTIVE JOBS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerActiveJobsScreen(
    onBackClick: () -> Unit,
    onJobClick: (String) -> Unit,
    onManageApplicantsClick: (String) -> Unit,
    onNavigateToPost: () -> Unit
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()

    val ownerActiveJobs = jobs.filter { 
        it.ownerId == currentUser?.id && (it.status == JobStatus.ACTIVE || it.status == JobStatus.FILLED)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = { Text("My Active Jobs", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            if (ownerActiveJobs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.WorkOutline, contentDescription = null, modifier = Modifier.size(32.dp), tint = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Active Jobs Posted", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("You haven't posted any active jobs yet.", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToPost,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Post a New Job", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Active Job Postings (${ownerActiveJobs.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(ownerActiveJobs, key = { it.id }) { job ->
                        val jobAppsCount = applications.count { it.jobId == job.id }

                        Card(
                            onClick = { onJobClick(job.id) },
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
                                    Text(
                                        text = job.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = PrimaryIndigo.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = job.status.name,
                                            color = PrimaryIndigo,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(job.location, fontSize = 13.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("₹${job.wage} / day", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = DarkSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "👥 $jobAppsCount Applicants",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onManageApplicantsClick(job.id) },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Manage Applicants", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

// --- 2. OWNER APPLICATIONS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerApplicationsScreen(
    onBackClick: () -> Unit,
    onWorkerClick: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()

    val ownerJobIds = jobs.filter { it.ownerId == currentUser?.id }.map { it.id }.toSet()
    val ownerApps = applications.filter { it.jobId in ownerJobIds || it.ownerId == currentUser?.id }

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = { Text("Job Applications", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Received Applications (${ownerApps.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AnimatedVisibility(visible = actionError != null) {
                    Text(
                        text = actionError ?: "",
                        color = WarningRose,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (ownerApps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(32.dp), tint = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Applications Received", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("You haven't received any applications for your jobs yet.", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ownerApps, key = { it.id }) { app ->
                            OwnerApplicationCard(
                                app = app,
                                onWorkerClick = onWorkerClick,
                                onNavigateToChat = onNavigateToChat,
                                onLoadingChange = { isActionLoading = it },
                                onErrorChange = { actionError = it },
                                scope = scope
                            )
                        }
                    }
                }
            }

            LoadingOverlay(isLoading = isActionLoading, text = "Updating status...")
        }
    }
}

// --- 3. WORKERS HIRED SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerHiredWorkersScreen(
    onBackClick: () -> Unit,
    onWorkerClick: (String) -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()

    val ownerJobIds = jobs.filter { it.ownerId == currentUser?.id }.map { it.id }.toSet()
    val hiredApps = applications.filter { 
        (it.jobId in ownerJobIds || it.ownerId == currentUser?.id) && 
        (it.status == ApplicationStatus.ACCEPTED || it.status == ApplicationStatus.COMPLETED)
    }

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = { Text("Workers Hired", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hired / Accepted Workers (${hiredApps.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AnimatedVisibility(visible = actionError != null) {
                    Text(
                        text = actionError ?: "",
                        color = WarningRose,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (hiredApps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = DarkSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(32.dp), tint = TextSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Workers Hired Yet", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Workers accepted for your posted jobs will appear here.", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(hiredApps, key = { it.id }) { app ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, BorderStrokeColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(app.labourName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SuccessGreen)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.labourName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextPrimary,
                                                modifier = Modifier.clickable { onWorkerClick(app.labourId) }
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${app.labourRating} • ${app.labourExperience} Exp", fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }

                                        Surface(
                                            color = SuccessGreen.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f)),
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = if (app.status == ApplicationStatus.ACCEPTED) "HIRED" else app.status.name,
                                                color = SuccessGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Surface(
                                        color = DarkSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Work, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Job: ${app.jobTitle}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Skills: ${app.labourSkills.joinToString(", ")}",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = {
                                                isActionLoading = true
                                                scope.launch {
                                                    RozgarRepository.getOrCreateConversation(
                                                        jobId = app.jobId,
                                                        applicationId = app.id,
                                                        targetUserId = app.labourId
                                                    ).collect { res ->
                                                        isActionLoading = false
                                                        res.fold(
                                                            onSuccess = { threadId -> onNavigateToChat(threadId) },
                                                            onFailure = { err -> actionError = err.localizedMessage }
                                                        )
                                                    }
                                                }
                                            },
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Email, contentDescription = "Contact Worker", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Contact Worker", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LoadingOverlay(isLoading = isActionLoading, text = "Opening chat...")
        }
    }
}

// --- REUSABLE APPLICATION CARD COMPONENT FOR OWNER ---

@Composable
fun OwnerApplicationCard(
    app: JobApplication,
    onWorkerClick: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onErrorChange: (String?) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Row 1: Profile & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(app.labourName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryIndigo)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.labourName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        modifier = Modifier.clickable { onWorkerClick(app.labourId) }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${app.labourRating} • ${app.labourExperience} Exp", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                if (app.status != ApplicationStatus.APPLIED) {
                    val isAccepted = app.status == ApplicationStatus.ACCEPTED
                    val bColor = if (isAccepted) SuccessGreen else WarningRose
                    Surface(
                        color = bColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, bColor.copy(alpha = 0.3f)),
                        shape = CircleShape
                    ) {
                        Text(
                            text = app.status.name,
                            color = bColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Job Title
            Text(
                text = "Applied for: ${app.jobTitle}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentCyan
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Trade: ${app.labourSkills.joinToString(", ")}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (app.status == ApplicationStatus.APPLIED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onLoadingChange(true)
                            scope.launch {
                                RozgarRepository.rejectApplicant(app.id).collect { result ->
                                    onLoadingChange(false)
                                    result.fold(
                                        onSuccess = {},
                                        onFailure = { err -> onErrorChange(err.localizedMessage) }
                                    )
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRose),
                        border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Reject", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (app.chatApproved) {
                        Button(
                            onClick = {
                                onLoadingChange(true)
                                scope.launch {
                                    RozgarRepository.getOrCreateConversation(
                                        jobId = app.jobId,
                                        applicationId = app.id,
                                        targetUserId = app.labourId
                                    ).collect { res ->
                                        onLoadingChange(false)
                                        res.fold(
                                            onSuccess = { threadId -> onNavigateToChat(threadId) },
                                            onFailure = { err -> onErrorChange(err.localizedMessage) }
                                        )
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.2f), contentColor = AccentCyan),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = "Chat Approved", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Chat Approved ✓", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                onLoadingChange(true)
                                scope.launch {
                                    RozgarRepository.approveChat(app.id).collect { result ->
                                        onLoadingChange(false)
                                        result.fold(
                                            onSuccess = {},
                                            onFailure = { err -> onErrorChange(err.localizedMessage) }
                                        )
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = "Approve Chat", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve Chat", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            onLoadingChange(true)
                            scope.launch {
                                RozgarRepository.acceptApplicant(app.id).collect { result ->
                                    onLoadingChange(false)
                                    result.fold(
                                        onSuccess = {},
                                        onFailure = { err -> onErrorChange(err.localizedMessage) }
                                    )
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        modifier = Modifier.weight(1.1f).height(40.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Hire", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hire Worker", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            } else if (app.status == ApplicationStatus.ACCEPTED || app.status == ApplicationStatus.COMPLETED) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            onLoadingChange(true)
                            scope.launch {
                                RozgarRepository.getOrCreateConversation(
                                    jobId = app.jobId,
                                    applicationId = app.id,
                                    targetUserId = app.labourId
                                ).collect { res ->
                                    onLoadingChange(false)
                                    res.fold(
                                        onSuccess = { threadId -> onNavigateToChat(threadId) },
                                        onFailure = { err -> onErrorChange(err.localizedMessage) }
                                    )
                                }
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = "Contact Worker", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contact Worker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
