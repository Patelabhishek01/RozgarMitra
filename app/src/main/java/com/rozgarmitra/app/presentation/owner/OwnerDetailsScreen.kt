package com.rozgarmitra.app.presentation.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDetailsScreen(
    onProfileCompleted: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                title = { Text("Owner Profile Details", fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    TextButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                RozgarRepository.skipOwnerProfile().collectLatest { result ->
                                    isLoading = false
                                    onProfileCompleted()
                                }
                            }
                        }
                    ) {
                        Text("Skip / छोड़ें", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    Text(
                        text = "Business Details / व्यावसायिक विवरण",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "Specify your work site address and company name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Address Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Work Site Address / कार्य स्थल का पता",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        
                        TextButton(
                            onClick = {
                                isLocating = true
                                scope.launch {
                                    delay(1200) // Simulating GPS locating
                                    address = "Flat 102, Shanti Vihar, Sector 4, HSR Layout, Bengaluru - 560102"
                                    isLocating = false
                                }
                            },
                            enabled = !isLocating
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isLocating) "Locating..." else "Use GPS", color = AccentCyan)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter full address or use GPS locator above", color = TextSecondary.copy(alpha = 0.5f)) },
                        minLines = 3,
                        maxLines = 5,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Company Name (Optional)
                    Text(
                        text = "Company or Trade Name (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        placeholder = { Text("Example: Sharma Contractors, Shop Owner", color = TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryLargeButton(
                        text = "Complete Profile / प्रोफ़ाइल पूरी करें",
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            
                            val flow = RozgarRepository.completeOwnerProfile(
                                address = address,
                                company = companyName
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
                        enabled = address.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                RozgarRepository.skipOwnerProfile().collectLatest { result ->
                                    isLoading = false
                                    onProfileCompleted()
                                }
                            }
                        }
                    ) {
                        Text("Skip for now / बाद में भरें", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            LoadingOverlay(isLoading = isLoading, text = "Saving Profile...")
        }
    }
}