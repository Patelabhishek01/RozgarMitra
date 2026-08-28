package com.rozgarmitra.app.presentation.job

import androidx.compose.animation.AnimatedVisibility
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Job details not found.", color = Color.Gray)
        }
        return
    }

    val isOwner = job.ownerId == currentUser?.id
    val hasApplied = applications.any { it.jobId == jobId && it.labourId == currentUser?.id }

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
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
                                    RozgarRepository.setPendingAction { onNavigateToChat("thread_" + job.ownerId) }
                                    onNavigateToLogin()
                                } else onNavigateToChat("thread_" + job.ownerId)
                            },
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    PrimaryLargeButton(
                        text = when {
                            isOwner -> "Manage Job"
                            hasApplied -> "Applied Successfully"
                            else -> "Apply for Job"
                        },
                        onClick = {
                            if (currentUser == null) {
                                RozgarRepository.setPendingAction {
                                    // This will trigger the apply logic once return
                                    // Actually, better to just return to details and let user click again?
                                    // User requirement: "Return to the same job/action"
                                }
                                onNavigateToLogin()
                            } else if (!isOwner && !hasApplied) {
                                isActionLoading = true
                                actionError = null
                                scope.launch {
                                    RozgarRepository.applyForJob(job.id).collectLatest { res ->
                                        isActionLoading = false
                                        res.fold(
                                            onSuccess = { /* Success */ },
                                            onFailure = { err -> actionError = err.localizedMessage }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = !hasApplied,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasApplied) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Category Tag
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        job.category.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (job.isUrgent) {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                "URGENT",
                                color = Color(0xFFD32F2F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Info Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoItem(icon = Icons.Filled.LocationOn, label = "Distance", value = "${String.format("%.1f", job.distanceKm)} km")
                    Spacer(modifier = Modifier.width(24.dp))
                    InfoItem(icon = Icons.Filled.Schedule, label = "Duration", value = "${job.durationDays} Days")
                    Spacer(modifier = Modifier.width(24.dp))
                    InfoItem(icon = Icons.Filled.Timer, label = "Daily Hours", value = "${job.hoursPerDay} Hrs")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Wage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offered Daily Wage", fontSize = 13.sp, color = Color(0xFF33691E))
                            Text("₹${job.wage}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        }
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Job Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Looking for experienced ${job.category} for work at ${job.location}. Work includes standard tasks related to ${job.category.lowercase()} trade. Materials will be provided at site.",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (job.skillsRequired.isNotEmpty()) {
                    Text("Required Skills", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        job.skillsRequired.forEach { skill ->
                            SuggestionChip(onClick = {}, label = { Text(skill) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Employer Profile
                Text("About the Employer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(job.ownerName.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(job.ownerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (job.ownerVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(size = 14)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${job.ownerRating} • Highly Rated", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Location Details
                Text("Work Location", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(job.location, color = Color.DarkGray)
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
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
