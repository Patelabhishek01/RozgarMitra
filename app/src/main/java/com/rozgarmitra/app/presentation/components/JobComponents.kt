package com.rozgarmitra.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.rozgarmitra.app.data.Job
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFeedCard(
    job: Job,
    onClick: () -> Unit,
    onApplyClick: () -> Unit
) {
    var isApplying by remember { mutableStateOf(false) }
    var applyError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val currentUser = RozgarRepository.currentUser.value
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val userApp = applications.firstOrNull { app -> app.jobId == job.id && app.labourId == currentUser?.id }

    val isFilled = job.status == com.rozgarmitra.app.data.JobStatus.FILLED || job.acceptedWorkersCount >= job.numberOfWorkersRequired

    val icon = when (job.category) {
        "Mason" -> Icons.Filled.Build
        "Electrician" -> Icons.Filled.FlashOn
        "Painter" -> Icons.Filled.Brush
        "Plumber" -> Icons.Filled.WaterDrop
        "Construction" -> Icons.Filled.Foundation
        "Carpenter" -> Icons.Filled.Handyman
        "Driver" -> Icons.Filled.DirectionsCar
        else -> Icons.Filled.Work
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PrimaryIndigo.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.25f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = job.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = job.category,
                            fontSize = 13.sp,
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (job.isUrgent) {
                    Surface(
                        color = WarningRose.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.3f)),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "⚡ URGENT",
                            color = Color(0xFFFB7185),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else if (isFilled) {
                    Surface(
                        color = TextMuted.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.25f)),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "FILLED",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata rows: Location, Date/Time, Workers needed
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${job.location} • ${String.format("%.1f", job.distanceKm)} km away",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                if (job.date.isNotBlank() || job.startTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${job.date.ifBlank { "Today" }} • ${job.startTime.ifBlank { "8:00 AM" }}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${job.numberOfWorkersRequired} workers needed (${job.acceptedWorkersCount} hired)",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = BorderStrokeColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${job.wage}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ ${job.wageType}",
                            fontSize = 12.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                val buttonText = when {
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.ACCEPTED -> "Accepted 🎉"
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.REJECTED -> "Not Selected"
                    userApp != null -> "Applied ✓"
                    isFilled -> "Job Filled"
                    else -> "Respond to Job"
                }

                val buttonColor = when {
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.ACCEPTED -> SuccessGreen
                    userApp != null -> PrimaryIndigo
                    isFilled -> DarkSurfaceVariant
                    else -> PrimaryIndigo
                }

                Button(
                    onClick = {
                        if (userApp != null || isFilled) {
                            onClick()
                        } else if (currentUser == null) {
                            onApplyClick()
                        } else {
                            isApplying = true
                            applyError = null
                            scope.launch {
                                RozgarRepository.applyForJob(job.id).collectLatest { result ->
                                    isApplying = false
                                    result.fold(
                                        onSuccess = {},
                                        onFailure = { error ->
                                            applyError = error.localizedMessage
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color.White),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp),
                    enabled = !isApplying && (userApp != null || !isFilled)
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            if (applyError != null) {
                Text(
                    text = applyError ?: "",
                    color = WarningRose,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}


