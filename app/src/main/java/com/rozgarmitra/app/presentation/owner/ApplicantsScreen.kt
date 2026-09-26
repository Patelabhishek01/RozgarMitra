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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.ApplicationStatus
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantsScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onWorkerClick: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    
    val job = jobs.firstOrNull { it.id == jobId }
    val jobApps = applications.filter { it.jobId == jobId }

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(job?.title?.take(20) ?: "Applicants", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(26.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Review Applicants (${jobApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                AnimatedVisibility(visible = actionError != null) {
                    Text(
                        text = actionError ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (jobApps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No applicants yet for this job.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(jobApps) { app ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Row 1: Profile snippet
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(app.labourName.take(1), fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.labourName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.clickable { onWorkerClick(app.labourId) }
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${app.labourRating} • ${app.labourExperience} Exp", fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        
                                        // Status badge
                                        if (app.status != ApplicationStatus.APPLIED) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (app.status == ApplicationStatus.ACCEPTED) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = app.status.name,
                                                    color = if (app.status == ApplicationStatus.ACCEPTED) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Trade: ${app.labourSkills.joinToString(", ")}",
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (app.status == ApplicationStatus.APPLIED) {
                                        // Decisions Buttons (Large click targets)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    isActionLoading = true
                                                    scope.launch {
                                                        RozgarRepository.rejectApplicant(app.id).collect { result ->
                                                            isActionLoading = false
                                                            result.fold(
                                                                onSuccess = {},
                                                                onFailure = { err -> actionError = err.localizedMessage }
                                                            )
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                                border = BorderStroke(1.dp, Color(0xFFC62828)),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Reject")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reject")
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Button(
                                                onClick = {
                                                    isActionLoading = true
                                                    scope.launch {
                                                        RozgarRepository.acceptApplicant(app.id).collect { result ->
                                                            isActionLoading = false
                                                            result.fold(
                                                                onSuccess = {},
                                                                onFailure = { err -> actionError = err.localizedMessage }
                                                            )
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = "Accept")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Hire Worker")
                                            }
                                        }
                                    } else {
                                        // Communicate option for matched worker
                                        if (app.status == ApplicationStatus.ACCEPTED) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                Button(
                                                    onClick = { onWorkerClick(app.labourId) },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Contact Worker")
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

            LoadingOverlay(isLoading = isActionLoading, text = "Updating status...")
        }
    }
}
