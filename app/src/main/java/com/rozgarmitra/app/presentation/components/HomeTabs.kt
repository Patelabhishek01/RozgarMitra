package com.rozgarmitra.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository

val professions = listOf("All Jobs", "Mason", "Painter", "Electrician", "Plumber", "Carpenter", "Construction", "Driver", "Cleaner", "Helper")

@Composable
fun HomeFeedTab(
    onJobClick: (String) -> Unit,
    onApplyClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onSeeAllUrgent: () -> Unit,
    onSeeAllNearby: () -> Unit,
    onSeeAllPopular: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    
    var selectedProfessions by remember { mutableStateOf(setOf("All Jobs")) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredJobs = if (selectedProfessions.contains("All Jobs")) {
        jobs
    } else {
        jobs.filter { it.category in selectedProfessions }
    }

    val urgentJobs = filteredJobs.filter { it.isUrgent }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header & Location
        item {
            HomeHeader(
                userName = currentUser?.name,
                role = currentUser?.role,
                onLoginClick = onNavigateToLogin,
                onNotificationClick = onNotificationClick
            )
        }

        // 2. Location Indicator (Marketplace feel)
        item {
            LocationIndicator(
                location = "HSR Layout, Bengaluru",
                onClick = { /* TODO: Select Location */ }
            )
        }

        // 3. Browse by Profession
        item {
            ProfessionFilterRow(
                selected = selectedProfessions,
                onSelectionChanged = { newSelection ->
                    selectedProfessions = newSelection
                },
                onFilterClick = { showFilterSheet = true }
            )
        }

        // 4. Urgent Jobs Section
        if (urgentJobs.isNotEmpty()) {
            item {
                SectionHeader(
                    title = if (selectedProfessions.size == 1 && !selectedProfessions.contains("All Jobs")) 
                        "Urgent ${selectedProfessions.first()} Jobs" else "Urgent Jobs",
                    onSeeAllClick = onSeeAllUrgent
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(urgentJobs, key = { "urgent_" + it.id.ifBlank { it.title } }) { job ->
                        Box(modifier = Modifier.width(300.dp)) {
                            JobFeedCard(job = job, onClick = { onJobClick(job.id) }, onApplyClick = onApplyClick)
                        }
                    }
                }
            }
        }

        // 5. All Active Jobs (Public feed)
        if (filteredJobs.isNotEmpty()) {
            item {
                SectionHeader(
                    title = if (selectedProfessions.size == 1 && !selectedProfessions.contains("All Jobs")) 
                        "${selectedProfessions.first()} Jobs" else "Active Jobs Posted by Owners",
                    onSeeAllClick = onSeeAllNearby
                )
            }
            items(filteredJobs, key = { it.id.ifBlank { it.title + it.createdAt } }) { job ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    JobFeedCard(job = job, onClick = { onJobClick(job.id) }, onApplyClick = onApplyClick)
                }
            }
        }
        
        // Empty State if no jobs found after filtering
        if (filteredJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No matching jobs found", fontWeight = FontWeight.Bold, color = Color.Gray)
                        TextButton(onClick = { 
                            selectedProfessions = setOf("All Jobs")
                            RozgarRepository.observeJobs()
                        }) {
                            Text("Browse All Jobs")
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }
}

@Composable
fun HomeHeader(
    userName: String?,
    role: Role?,
    onLoginClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (userName != null) "Namaste, ${userName.split(" ").first()}! 👋" else "Namaste! 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            if (role != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (role == Role.OWNER) "EMPLOYER / OWNER" else "WORKER / LABOUR",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text("Find your work for today", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Filled.NotificationsNone, contentDescription = "Notifications")
            }
            if (userName == null) {
                Button(
                    onClick = onLoginClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Login", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LocationIndicator(location: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(location, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionFilterRow(
    selected: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onFilterClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Browse by Profession",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            IconButton(onClick = onFilterClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(professions) { profession ->
                val isSelected = selected.contains(profession)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSelection = when {
                            profession == "All Jobs" -> setOf("All Jobs")
                            isSelected -> {
                                val updated = selected - profession
                                if (updated.isEmpty()) setOf("All Jobs") else updated
                            }
                            else -> {
                                (selected - "All Jobs") + profession
                            }
                        }
                        onSelectionChanged(newSelection)
                    },
                    label = { Text(profession) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.LightGray,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp
                    )
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        TextButton(onClick = onSeeAllClick) {
            Text("See All →", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchTab(
    onJobClick: (String) -> Unit,
    onApplyClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()
    val popularProfessions = listOf("Mason", "Painter", "Plumber", "Electrician", "Carpenter", "Driver")

    val filteredJobs = jobs.filter { job ->
        job.title.contains(searchQuery, ignoreCase = true) ||
                job.category.contains(searchQuery, ignoreCase = true) ||
                job.location.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Search Jobs",
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
        
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Profession, skills, or location") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        if (searchQuery.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Popular Professions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularProfessions.forEach { prof ->
                        SuggestionChip(
                            onClick = { searchQuery = prof },
                            label = { Text(prof) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Text("Recent Searches", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                listOf("Mason in HSR", "Painter jobs").forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { searchQuery = recent }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(recent, color = Color.DarkGray)
                    }
                }
            }
        } else {
            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No results for \"$searchQuery\"", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredJobs) { job ->
                        JobFeedCard(
                            job = job,
                            onClick = { onJobClick(job.id) },
                            onApplyClick = onApplyClick
                        )
                    }
                }
            }
        }
    }
}
