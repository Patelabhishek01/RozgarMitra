package com.rozgarmitra.app.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfessionsScreen(
    onBackClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val allProfessions = listOf("Mason", "Painter", "Electrician", "Plumber", "Carpenter", "Construction", "Driver", "Cleaner", "Helper")
    
    var selectedProfessions by remember(currentUser) { 
        mutableStateOf(currentUser?.labourProfile?.skills?.toSet() ?: emptySet()) 
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text("My Professions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .background(DarkBackground)
                    .padding(16.dp)
            ) {
                PrimaryLargeButton(
                    text = "Save Changes",
                    onClick = { onBackClick() }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Select categories you are skilled in. This helps us show you relevant work.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(allProfessions) { prof ->
                val isSelected = selectedProfessions.contains(prof)
                ProfessionItem(
                    label = prof,
                    isSelected = isSelected,
                    onToggle = {
                        selectedProfessions = if (isSelected) selectedProfessions - prof else selectedProfessions + prof
                    }
                )
            }
        }
    }
}

@Composable
fun ProfessionItem(label: String, isSelected: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else DarkSurface,
        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryBlue)
            }
        }
    }
}

