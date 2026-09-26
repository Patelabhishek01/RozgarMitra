package com.rozgarmitra.app.presentation.job

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.JobFeedCard
import com.rozgarmitra.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobResultsScreen(
    title: String,
    filterType: String,
    onBackClick: () -> Unit,
    onJobClick: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    
    val filteredJobs = when (filterType) {
        "urgent" -> jobs.filter { it.isUrgent }
        "nearby" -> jobs.sortedBy { it.distanceKm }
        "popular" -> jobs.sortedByDescending { it.ownerRating }
        else -> {
            if (filterType != "all") {
                jobs.filter { it.category.equals(filterType, ignoreCase = true) }
            } else jobs
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                ),
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Filter */ }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = AccentCyan)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(paddingValues), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff, 
                        contentDescription = null, 
                        modifier = Modifier.size(64.dp), 
                        tint = TextSecondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No jobs found", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredJobs) { job ->
                    JobFeedCard(
                        job = job,
                        onClick = { onJobClick(job.id) },
                        onApplyClick = {
                            RozgarRepository.setPendingAction { }
                            onNavigateToLogin()
                        }
                    )
                }
            }
        }
    }
}

