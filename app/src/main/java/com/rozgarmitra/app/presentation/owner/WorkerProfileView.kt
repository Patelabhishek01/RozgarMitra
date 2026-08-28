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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.ApplicationStatus
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.VerifiedBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkerProfileView(
    workerId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    
    // We will search for this worker in applications or mock profiles.
    // In our mock database, worker_1 is "Ramesh Kumar". worker_2 is Lokesh Yadav.
    val workerName = if (workerId == "worker_1") "Ramesh Kumar" else "Lokesh Yadav"
    val skills = if (workerId == "worker_1") listOf("Mason", "Construction Labour") else listOf("Mason", "Plastering")
    val experience = if (workerId == "worker_1") "5 years" else "3 years"
    val expectedWage = if (workerId == "worker_1") 650 else 550
    val rating = if (workerId == "worker_1") 4.8f else 4.5f
    val phone = if (workerId == "worker_1") "9876543210" else "9988776655"
    
    // Verify match status
    val appMatch = applications.firstOrNull { it.labourId == workerId && it.status == ApplicationStatus.ACCEPTED }
    val isMatched = appMatch != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worker Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(26.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(workerName.take(1), fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(workerName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            VerifiedBadge(size = 18)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$rating rating", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(24.dp))

                // Privacy lock phone warning
                Text("Contact Details / संपर्क विवरण", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isMatched) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Mobile Number", fontSize = 12.sp, color = Color.Gray)
                                Text("+91 $phone", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Mobile Number (Hidden)", fontSize = 12.sp, color = Color.Gray)
                                Text("+91 XXXXX XXXXX", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text("Only shared after worker is hired.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trades/Skills
                Text("Skills & Trades / कौशल", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
                        SuggestionChip(onClick = {}, label = { Text(skill) })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Experience & Wage metrics
                Row(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Experience", fontSize = 11.sp, color = Color.Gray)
                            Text(experience, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Expected Daily Wage", fontSize = 11.sp, color = Color.Gray)
                            Text("₹$expectedWage / Day", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryLargeButton(
                text = "Message Worker / संदेश भेजें",
                onClick = {
                    onNavigateToChat("thread_" + workerId)
                },
                icon = Icons.Filled.Chat
            )
        }
    }
}
