package com.rozgarmitra.app.presentation.worker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

val tradesList = listOf(
    "Mason (राजमिस्त्री)",
    "Painter (चित्रकार)",
    "Electrician (बिजली मिस्त्री)",
    "Plumber (नलसाज)",
    "Welder (वेल्डर)",
    "Driver (चालक)",
    "Cleaner (सफाई कर्मचारी)",
    "Gardener (माली)",
    "Farm Worker (किसान)",
    "Construction Labour (मजदूर)",
    "Helper (सहायक)",
    "Cook (रसोइया)",
    "Security Guard (सुरक्षा गार्ड)",
    "Mechanic (मैकेनिक)"
)

val experienceTiers = listOf(
    "No Experience (कोई अनुभव नहीं)",
    "1 - 2 Years",
    "3 - 5 Years",
    "5+ Years"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LabourDetailsScreen(
    onProfileCompleted: () -> Unit
) {
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    var selectedExperience by remember { mutableStateOf("") }
    var expectedWage by remember { mutableStateOf("600") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { snackbarHostState },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Labour Profile Details", fontWeight = FontWeight.Bold) }
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Work Details / काम का विवरण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Select your skills so owners can find you easily.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Skills Grid (Chips)
                    Text(
                        text = "Select Skills / कौशल चुनें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tradesList.forEach { skill ->
                            val isSelected = selectedSkills.contains(skill)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSkills = if (isSelected) {
                                        selectedSkills - skill
                                    } else {
                                        selectedSkills + skill
                                    }
                                },
                                label = { Text(skill, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Experience Tier
                    Text(
                        text = "Your Experience / आपका अनुभव",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        experienceTiers.forEach { tier ->
                            val isSelected = selectedExperience == tier
                            OutlinedCard(
                                onClick = { selectedExperience = tier },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = tier,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Expected Daily Wage
                    Text(
                        text = "Expected Daily Wage / अपेक्षित दैनिक मजदूरी (₹)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = expectedWage,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    expectedWage = it
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Wage preset quick buttons
                        listOf("400", "600", "800").forEach { preset ->
                            OutlinedButton(
                                onClick = { expectedWage = preset },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("₹$preset")
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryLargeButton(
                    text = "Complete Profile / प्रोफ़ाइल पूरी करें",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        
                        val flow = RozgarRepository.completeLabourProfile(
                            skills = selectedSkills.map { it.substringBefore(" (") },
                            experience = selectedExperience.substringBefore(" ("),
                            expectedWage = expectedWage.toIntOrNull() ?: 0
                        )
                        scope.launch {
                            flow.collectLatest { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = {
                                        onProfileCompleted()
                                    },
                                    onFailure = { error ->
                                        errorMessage = error.localizedMessage ?: "Failed to save profile."
                                    }
                                )
                            }
                        }
                    },
                    enabled = selectedSkills.isNotEmpty() && selectedExperience.isNotBlank() && expectedWage.isNotBlank()
                )
            }

            LoadingOverlay(isLoading = isLoading, text = "Saving Profile...")
        }
    }
}