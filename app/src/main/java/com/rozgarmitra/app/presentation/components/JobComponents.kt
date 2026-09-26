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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFEFEFEF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = job.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = job.category,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (job.isUrgent) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "URGENT",
                            color = Color(0xFFD32F2F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isFilled) {
                    Surface(
                        color = Color(0xFFECEFF1),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "FILLED",
                            color = Color(0xFF455A64),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata rows: Location, Date/Time, Workers needed, Distance
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${job.location} • ${String.format("%.1f", job.distanceKm)} km away",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

                if (job.date.isNotBlank() || job.startTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Event, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${job.date.ifBlank { "Today" }} • ${job.startTime.ifBlank { "8:00 AM" }}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Group, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${job.numberOfWorkersRequired} workers needed (${job.acceptedWorkersCount} hired)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${job.wage}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "/ ${job.wageType}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                val buttonText = when {
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.ACCEPTED -> "Accepted 🎉"
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.REJECTED -> "Not Selected"
                    userApp != null -> "Response Sent ✓"
                    isFilled -> "Job Filled"
                    else -> "Respond to Job"
                }

                val buttonColor = when {
                    userApp?.status == com.rozgarmitra.app.data.ApplicationStatus.ACCEPTED -> Color(0xFF2E7D32)
                    userApp != null -> Color(0xFF1565C0)
                    isFilled -> Color.Gray
                    else -> MaterialTheme.colorScheme.primary
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
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(42.dp),
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
                    color = Color.Red,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

