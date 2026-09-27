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
import androidx.compose.material.icons.filled.Email
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
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicantsScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onWorkerClick: (String) -> Unit,
    onNavigateToChat: (String) -> Unit = {}
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    
    val job = jobs.firstOrNull { it.id == jobId }
    val jobApps = applications.filter { it.jobId == jobId }

    var isActionLoading by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                title = { Text(job?.title?.take(20) ?: "Applicants", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
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
                    text = "Review Applicants (${jobApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
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

                if (jobApps.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No applicants yet for this job.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(jobApps) { app ->
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, BorderStrokeColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    // Row 1: Profile snippet
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
                                        
                                        // Status badge
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

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Trade: ${app.labourSkills.joinToString(", ")}",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (app.status == ApplicationStatus.APPLIED) {
                                        // Decision Buttons: Reject, Approve Chat, Hire Worker
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
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
                                                        isActionLoading = true
                                                        scope.launch {
                                                            RozgarRepository.approveChat(app.id).collect { result ->
                                                                isActionLoading = false
                                                                result.fold(
                                                                    onSuccess = {},
                                                                    onFailure = { err -> actionError = err.localizedMessage }
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
                                    } else {
                                        if (app.status == ApplicationStatus.ACCEPTED || app.status == ApplicationStatus.COMPLETED) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                        }
                    }
                }
            }

            LoadingOverlay(isLoading = isActionLoading, text = "Updating status...")
        }
    }
}

