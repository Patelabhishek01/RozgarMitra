package com.rozgarmitra.app.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.presentation.components.getLocalizedTradeName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfessionsScreen(
    onBackClick: () -> Unit
) {
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val allProfessions = listOf("Mason", "Painter", "Electrician", "Plumber", "Carpenter", "Construction", "Driver", "Cleaner", "Helper")
    
    var selectedProfessions by remember(currentUser) { 
        mutableStateOf(currentUser?.labourProfile?.skills?.toSet() ?: emptySet()) 
    }
    var experienceText by remember(currentUser) {
        mutableStateOf(currentUser?.labourProfile?.experience ?: "")
    }
    var expectedWageText by remember(currentUser) {
        mutableStateOf(currentUser?.labourProfile?.expectedWage?.takeIf { it > 0 }?.toString() ?: "")
    }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text(stringResource(R.string.menu_my_professions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
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
                    text = stringResource(R.string.action_save_changes),
                    onClick = {
                        isSaving = true
                        val wageInt = expectedWageText.toIntOrNull() ?: 0
                        scope.launch {
                            RozgarRepository.updateLabourProfessions(
                                skills = selectedProfessions.toList(),
                                experience = experienceText,
                                wage = wageInt
                            ).collectLatest { res ->
                                isSaving = false
                                if (res.isSuccess) {
                                    onBackClick()
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.work_details),
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontSize = 14.sp
                    )
                }

                item {
                    OutlinedTextField(
                        value = experienceText,
                        onValueChange = { experienceText = it },
                        label = { Text(stringResource(R.string.years_of_experience), color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStrokeColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = expectedWageText,
                        onValueChange = { expectedWageText = it },
                        label = { Text(stringResource(R.string.expected_daily_wage), color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderStrokeColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.select_skills),
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontSize = 14.sp
                    )
                    Text(
                        stringResource(R.string.select_skills_hint),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }
                
                items(allProfessions) { prof ->
                    val isSelected = selectedProfessions.contains(prof)
                    ProfessionItem(
                        label = getLocalizedTradeName(prof),
                        isSelected = isSelected,
                        onToggle = {
                            selectedProfessions = if (isSelected) selectedProfessions - prof else selectedProfessions + prof
                        }
                    )
                }
            }

            LoadingOverlay(isLoading = isSaving, text = stringResource(R.string.action_please_wait))
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
