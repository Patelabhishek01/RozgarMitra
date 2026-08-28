package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phone: String,
    onVerificationSuccess: (Boolean) -> Unit, // passes whether user profile is completed
    onBackClick: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableStateOf(30) }

    // Resend Timer countdown
    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Verify Mobile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp) // Large tap size!
                        )
                    }
                }
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "We sent an OTP code to",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = "+91 $phone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                otpCode = it
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(68.dp),
                        textStyle = TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 8.sp
                        ),
                        placeholder = {
                            Text(
                                "123456",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    color = Color.LightGray,
                                    letterSpacing = 8.sp
                                )
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter '123456' to proceed (Demo OTP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (timerSeconds > 0) {
                            Text(
                                text = "Resend OTP in ${timerSeconds}s",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    timerSeconds = 30
                                    // Simulated OTP resend
                                    RozgarRepository.addNotification("OTP Resent", "A new OTP code has been sent to +91 $phone.")
                                }
                            ) {
                                Text(
                                    "Resend Code",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                PrimaryLargeButton(
                    text = "Verify & Continue",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        
                        // Launch coroutine flow call
                        val flow = RozgarRepository.verifyOtp(phone, otpCode)
                        scope.launch {
                            flow.collectLatest { result ->
                                isLoading = false
                                result.fold(
                                    onSuccess = { user ->
                                        if (user != null) {
                                            RozgarRepository.executePendingAction()
                                            onVerificationSuccess(user.profileCompleted)
                                        } else {
                                            // User is not registered, navigate to Register
                                            onVerificationSuccess(false)
                                        }
                                    },
                                    onFailure = { error ->
                                        errorMessage = error.localizedMessage ?: "Verification failed."
                                    }
                                )
                            }
                        }
                    },
                    enabled = otpCode.length >= 4,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            LoadingOverlay(isLoading = isLoading, text = "Verifying Code...")
        }
    }
}
