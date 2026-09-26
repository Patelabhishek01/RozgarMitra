package com.rozgarmitra.app.presentation.worker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.delay
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

    // Suggestion states
    var isSuggestingOther by remember { mutableStateOf(false) }
    var suggestedCategoryName by remember { mutableStateOf("") }
    var suggestedCategoryDesc by remember { mutableStateOf("") }
    var suggestionLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DarkSurface,
        unfocusedContainerColor = DarkSurface,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = BorderStrokeColor,
        focusedLabelColor = AccentCyan,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBackground),
                title = { Text("Labour Profile Details", fontWeight = FontWeight.Bold, color = TextPrimary) }
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
                    Text(
                        text = "Work Details / काम का विवरण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Select your skills so owners can find you easily.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Skills Grid (Chips)
                    Text(
                        text = "Select Skills / कौशल चुनें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tradesWithOther = tradesList + "Other (अन्य)"
                        
                        tradesWithOther.forEach { skill ->
                            val isSelected = selectedSkills.contains(skill)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (skill == "Other (अन्य)") {
                                        isSuggestingOther = true
                                    } else {
                                        selectedSkills = if (isSelected) {
                                            selectedSkills - skill
                                        } else {
                                            selectedSkills + skill
                                        }
                                    }
                                },
                                label = { Text(skill, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue.copy(alpha = 0.25f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = if (skill == "Other (अन्य)") PrimaryBlue.copy(alpha = 0.15f) else DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = BorderStrokeColor,
                                    selectedBorderColor = PrimaryBlue
                                )
                            )



                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Experience Tier
                    Text(
                        text = "Your Experience / आपका अनुभव",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
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
                                    containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.25f) else DarkSurface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryBlue else BorderStrokeColor
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
                                        color = TextPrimary,
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
                        fontSize = 15.sp,
                        color = TextPrimary
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
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SuccessGreen) }
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Wage preset quick buttons
                        listOf("400", "600", "800").forEach { preset ->
                            OutlinedButton(
                                onClick = { expectedWage = preset },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = DarkSurface,
                                    contentColor = SuccessGreen
                                ),
                                border = BorderStroke(1.dp, BorderStrokeColor),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("₹$preset", fontWeight = FontWeight.Bold)
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
                            wage = expectedWage.toIntOrNull() ?: 0
                        )
                        scope.launch {
                            flow.collectLatest { result ->
                                result.fold(
                                    onSuccess = {
                                        delay(500)
                                        isLoading = false
                                        onProfileCompleted()
                                    },
                                    onFailure = { error ->
                                        isLoading = false
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

    if (isSuggestingOther) {
        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { isSuggestingOther = false },
            title = { Text("Suggest New Skill", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    Text("Can't find your skill? Tell us what you do.", fontSize = 13.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = suggestedCategoryName,
                        onValueChange = { suggestedCategoryName = it },
                        label = { Text("Skill Name (e.g. Driver)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suggestedCategoryDesc,
                        onValueChange = { suggestedCategoryDesc = it },
                        label = { Text("What work do you do in this?") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        suggestionLoading = true
                        scope.launch {
                            RozgarRepository.suggestNewCategory(suggestedCategoryName, suggestedCategoryDesc).collect { res ->
                                suggestionLoading = false
                                res.onSuccess {
                                    isSuggestingOther = false
                                    errorMessage = "Skill suggested! We will add it to the list soon."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                    enabled = suggestedCategoryName.isNotBlank() && suggestedCategoryDesc.isNotBlank() && !suggestionLoading
                ) {
                    if (suggestionLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSuggestingOther = false }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

