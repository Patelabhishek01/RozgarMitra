package com.rozgarmitra.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.ui.theme.*

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
    val userLocationData by RozgarRepository.userLocationData.collectAsStateWithLifecycle()
    val searchRadiusKm by RozgarRepository.searchRadiusKm.collectAsStateWithLifecycle()
    
    var selectedProfessions by remember { mutableStateOf(setOf("All Jobs")) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val displayLocation = userLocationData?.addressName?.ifBlank { "Location Available" } ?: "Select Location"

    val filteredJobs = jobs.filter { job ->
        val matchesCategory = selectedProfessions.contains("All Jobs") || job.category in selectedProfessions
        val matchesRadius = searchRadiusKm == 0.0 || job.distanceKm == 0.0 || job.distanceKm <= searchRadiusKm
        matchesCategory && matchesRadius
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
                location = displayLocation,
                radiusKm = searchRadiusKm,
                onClick = { showLocationDialog = true }
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
                        "Urgent ${selectedProfessions.first()} Jobs" else "⚡ Urgent Jobs",
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
                        "${selectedProfessions.first()} Jobs" else "Active Jobs Near You",
                    onSeeAllClick = onSeeAllNearby
                )
            }
            items(filteredJobs, key = { it.id.ifBlank { it.title + it.createdAt } }) { job ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    JobFeedCard(job = job, onClick = { onJobClick(job.id) }, onApplyClick = onApplyClick)
                }
            }
        }
        
        // Empty State if no jobs found after filtering
        if (filteredJobs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(32.dp), tint = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No matching jobs found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Text("Try adjusting your profession filter or location", fontSize = 13.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { 
                            selectedProfessions = setOf("All Jobs")
                            RozgarRepository.observeJobs()
                        }) {
                            Text("Browse All Jobs", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
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

    if (showLocationDialog) {
        LocationSelectionDialog(
            onDismiss = { showLocationDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHeader(
    userName: String?,
    role: Role?,
    onLoginClick: () -> Unit,
    onNotificationClick: () -> Unit,
    unreadNotifsCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (userName != null) "Namaste, ${userName.split(" ").first()}! 👋" else "Namaste! 👋",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (role != null) {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.3f)),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (role == Role.OWNER) "EMPLOYER / OWNER" else "WORKER / LABOUR",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                }
            } else {
                Text("Find daily work near you", fontSize = 13.sp, color = TextSecondary)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = DarkSurface,
                border = BorderStroke(1.dp, BorderStrokeColor),
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = onNotificationClick) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifsCount > 0) {
                                Badge(containerColor = WarningRose) { Text(unreadNotifsCount.toString(), color = Color.White) }
                            }
                        }
                    ) {
                        Icon(Icons.Filled.NotificationsNone, contentDescription = "Notifications", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (userName == null) {
                Button(
                    onClick = onLoginClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("Login", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun LocationIndicator(location: String, radiusKm: Double = 0.0, onClick: () -> Unit) {
    val radiusLabel = if (radiusKm > 0.0) " • Within ${radiusKm.toInt()} km" else ""
    Surface(
        onClick = onClick,
        color = DarkSurface,
        shape = CircleShape,
        border = BorderStroke(1.dp, BorderStrokeColor),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(location + radiusLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
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
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Browse by Profession",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Surface(
                shape = CircleShape,
                color = DarkSurface,
                border = BorderStroke(1.dp, BorderStrokeColor),
                modifier = Modifier.size(32.dp).clickable(onClick = onFilterClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filter", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                }
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
                    label = { Text(profession, fontWeight = FontWeight.Medium) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryIndigo,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = BorderStrokeColor,
                        selectedBorderColor = PrimaryIndigo,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
        TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(0.dp)) {
            Text("See All →", fontWeight = FontWeight.Bold, color = PrimaryIndigo, fontSize = 13.sp)
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
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by profession, skills, or area...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryIndigo) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = DarkSurface,
                    unfocusedBorderColor = BorderStrokeColor,
                    focusedBorderColor = PrimaryIndigo,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        if (searchQuery.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Popular Professions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularProfessions.forEach { prof ->
                        SuggestionChip(
                            onClick = { searchQuery = prof },
                            label = { Text(prof, color = TextPrimary) },
                            shape = CircleShape,
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkSurface),
                            border = SuggestionChipDefaults.suggestionChipBorder(borderColor = BorderStrokeColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text("Recent Searches", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                listOf("Mason in HSR", "Painter jobs").forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { searchQuery = recent }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(recent, color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        } else {
            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, modifier = Modifier.size(56.dp), tint = TextMuted)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No results for \"$searchQuery\"", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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

