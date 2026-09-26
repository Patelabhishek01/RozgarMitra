package com.rozgarmitra.app.presentation.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
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
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.VerifiedBadge
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkerProfileView(
    workerId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Search for worker in applications
    val appMatch = applications.firstOrNull { it.labourId == workerId }
    val workerName = appMatch?.labourName ?: "Worker"
    val skills = appMatch?.labourSkills?.ifEmpty { listOf("Daily Labour", "Helper") } ?: listOf("Daily Labour", "Helper")
    val experience = appMatch?.labourExperience?.ifBlank { "2+ years" } ?: "2+ years"
    val rating = if (appMatch?.labourRating ?: 0f > 0) appMatch!!.labourRating else 4.5f
    val phone = appMatch?.labourPhone ?: ""

    val isMatched = appMatch?.status == ApplicationStatus.ACCEPTED

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text("Worker Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(26.dp), tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header profile card
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(workerName.take(1), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(workerName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge(size = 18)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$rating rating", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = BorderStrokeColor)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Privacy lock phone warning
                    Text("Contact Details / संपर्क विवरण", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isMatched) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Mobile Number", fontSize = 12.sp, color = TextSecondary)
                                    Text(phone.ifBlank { "+91 9876543210" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, BorderStrokeColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = TextSecondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Mobile Number (Hidden)", fontSize = 12.sp, color = TextSecondary)
                                    Text("+91 XXXXX XXXXX", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
                                    Text("Shared after worker is hired.", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trades/Skills
                    Text("Skills & Trades / कौशल", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        skills.forEach { skill ->
                            SuggestionChip(
                                onClick = {}, 
                                label = { Text(skill, color = TextPrimary) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurface),
                                border = SuggestionChipDefaults.suggestionChipBorder(borderColor = BorderStrokeColor)
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Experience & Wage metrics
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, BorderStrokeColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Experience", fontSize = 11.sp, color = TextSecondary)
                                Text(experience, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = BorderStroke(1.dp, BorderStrokeColor)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Status", fontSize = 11.sp, color = TextSecondary)
                                Text(if (isMatched) "Hired" else "Applied", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (isMatched) SuccessGreen else TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryLargeButton(
                    text = "Message Worker / संदेश भेजें",
                    onClick = {
                        isLoading = true
                        scope.launch {
                            RozgarRepository.getOrCreateConversation(
                                jobId = appMatch?.jobId ?: "",
                                applicationId = appMatch?.id ?: "",
                                targetUserId = workerId
                            ).collect { res ->
                                isLoading = false
                                res.fold(
                                    onSuccess = { threadId -> onNavigateToChat(threadId) },
                                    onFailure = { }
                                )
                            }
                        }
                    },
                    icon = Icons.Filled.Chat
                )
            }

            LoadingOverlay(isLoading = isLoading, text = "Opening Chat...")
        }
    }
}


