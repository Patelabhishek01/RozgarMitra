package com.rozgarmitra.app.presentation.profile

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
        topBar = {
            TopAppBar(
                title = { Text("My Professions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { 
                    // TODO: Persist in repository
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Select categories you are skilled in. This helps us show you relevant work.",
                    fontSize = 14.sp,
                    color = Color.Gray,
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
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
