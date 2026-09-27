package com.rozgarmitra.app.presentation.job

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.*
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.VerifiedBadge
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToRating: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    
    val job = jobs.firstOrNull { it.id == jobId }
    
    if (job == null) {
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
            Text("Job details not found.", color = TextSecondary)
        }
        return
    }

    val isOwner = job.ownerId == currentUser?.id
    val userApp = applications.firstOrNull { it.jobId == jobId && it.labourId == currentUser?.id }
    val hasApplied = userApp != null
    val isFilled = job.status == JobStatus.FILLED || job.acceptedWorkersCount >= job.numberOfWorkersRequired || job.status == JobStatus.CLOSED

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = { Text("Job Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, BorderStrokeColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isOwner) {
                        OutlinedIconButton(
                            onClick = {
                                if (currentUser == null) {
                                    onNavigateToLogin()
                                } else {
                                    val app = userApp
                                    val canChat = app != null && (
                                        app.status == ApplicationStatus.ACCEPTED ||
                                        app.status == ApplicationStatus.COMPLETED ||
                                        app.chatApproved
                                    )
                                    when {
                                        app == null -> {
                                            actionError = "Apply to this job first to start chatting with the employer."
                                        }
                                        canChat -> {
                                            scope.launch {
                                                RozgarRepository.getOrCreateConversation(
                                                    jobId = job.id,
                                                    applicationId = app.id,
                                                    targetUserId = job.ownerId
                                                ).collect { res ->
                                                    res.fold(
                                                        onSuccess = { threadId -> onNavigateToChat(threadId) },
                                                        onFailure = { err -> actionError = err.localizedMessage }
                                                    )
                                                }
                                            }
                                        }
                                        app.status == ApplicationStatus.REJECTED || app.status == ApplicationStatus.CANCELLED -> {
                                            actionError = "Chat is unavailable for this application."
                                        }
                                        else -> {
                                            actionError = "Chat will be available after your application is accepted or chat is approved."
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, BorderStrokeColor)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Chat Owner", tint = PrimaryIndigo)
                        }
                    }

                    val buttonText = when {
                        isOwner -> "Manage Applicants"
                        userApp?.status == ApplicationStatus.ACCEPTED -> "Accepted 🎉"
                        userApp?.status == ApplicationStatus.REJECTED -> "Application Rejected"
                        userApp != null -> "Response Sent ✓"
                        isFilled -> "Job Filled"
                        else -> "Respond to Job"
                    }

                    val buttonColor = when {
                        userApp?.status == ApplicationStatus.ACCEPTED -> SuccessGreen
                        userApp?.status == ApplicationStatus.REJECTED -> WarningRose
                        userApp != null -> PrimaryIndigo
                        isFilled -> DarkSurfaceVariant
                        else -> PrimaryIndigo
                    }

                    PrimaryLargeButton(
                        text = buttonText,
                        onClick = {
                            if (isOwner) {
                                onBackClick()
                            } else if (currentUser == null) {
                                onNavigateToLogin()
                            } else if (!hasApplied && !isFilled) {
                                isActionLoading = true
                                actionError = null
                                scope.launch {
                                    RozgarRepository.applyForJob(job.id).collectLatest { res ->
                                        isActionLoading = false
                                        res.fold(
                                            onSuccess = { },
                                            onFailure = { err -> actionError = err.localizedMessage }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = isOwner || (!hasApplied && !isFilled),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(DarkBackground)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                AnimatedVisibility(visible = actionError != null) {
                    Text(
                        text = actionError ?: "",
                        color = WarningRose,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Category Tag
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        job.category.uppercase(),
                        color = PrimaryIndigo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        job.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (job.isUrgent) {
                        Surface(color = WarningRose.copy(alpha = 0.15f), border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.3f)), shape = CircleShape) {
                            Text(
                                "⚡ URGENT",
                                color = WarningRose,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else if (isFilled) {
                        Surface(color = TextMuted.copy(alpha = 0.15f), border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.3f)), shape = CircleShape) {
                            Text(
                                "FILLED",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Key Info Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem(icon = Icons.Filled.LocationOn, label = "Distance", value = "${String.format("%.1f", job.distanceKm)} km")
                    InfoItem(icon = Icons.Filled.Event, label = "Date & Time", value = "${job.date.ifBlank { "Today" }} • ${job.startTime.ifBlank { "8 AM" }}")
                    InfoItem(icon = Icons.Filled.Group, label = "Workers", value = "${job.acceptedWorkersCount}/${job.numberOfWorkersRequired} Hired")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offered Wage", fontSize = 13.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("₹${job.wage} / ${job.wageType}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                        }
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Payments, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Job Description", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Looking for experienced ${job.category} for work at ${job.location}. Work includes standard tasks related to ${job.category.lowercase()} trade. Materials will be provided at site.",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (job.skillsRequired.isNotEmpty()) {
                    Text("Required Skills", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        job.skillsRequired.forEach { skill ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(skill, color = TextPrimary) },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurface),
                                border = SuggestionChipDefaults.suggestionChipBorder(borderColor = BorderStrokeColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Employer Profile
                Text("About the Employer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = PrimaryIndigo.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(job.ownerName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryIndigo)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(job.ownerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                if (job.ownerVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(size = 14)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${job.ownerRating} • Highly Rated", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Location Details
                Text("Work Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(job.location, color = TextSecondary, fontSize = 14.sp)
                }
                if (job.distanceKm > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Navigation, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${String.format(java.util.Locale.US, "%.1f", job.distanceKm)} km away from your location", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }


                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    LoadingOverlay(isLoading = isActionLoading, text = "Submitting Application...")
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = TextMuted)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.padding(top = 2.dp))
    }
}

