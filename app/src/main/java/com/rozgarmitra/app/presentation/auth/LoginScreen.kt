package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSendOtpSuccess: (String) -> Unit,
    onLanguageSelectClick: () -> Unit
) {
    var mobileNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                // Header Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Text(
                        text = "RozgarMitra",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 36.sp
                    )

                    Text(
                        text = "Connecting Hands with Opportunities.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
                    )

                    Text(
                        text = "Welcome / स्वागत है!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Enter your mobile number to login or register.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Mobile Field
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = {
                            if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                mobileNumber = it
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        label = { Text("Mobile Number / मोबाइल नंबर") },
                        placeholder = { Text("Enter 10-digit number") },
                        prefix = {
                            Text(
                                text = "+91 ",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        shape = MaterialTheme.shapes.medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use '9876543210' for Worker, '8888888888' for Owner demo accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Footer Content (Buttons)
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryLargeButton(
                        text = "Send OTP / ओटीपी भेजें",
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            
                            val flow = RozgarRepository.login(context as Activity, mobileNumber)
                            scope.launch {
                                flow.collectLatest { result ->
                                    isLoading = false
                                    result.fold(
                                        onSuccess = {
                                            onSendOtpSuccess(mobileNumber)
                                        },
                                        onFailure = { error ->
                                            errorMessage = error.localizedMessage ?: "Something went wrong."
                                        }
                                    )
                                }
                            }
                        },
                        enabled = mobileNumber.length == 10
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Change Language / भाषा बदलें",
                            modifier = Modifier
                                .clickable { onLanguageSelectClick() }
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LoadingOverlay(isLoading = isLoading, text = "Sending OTP...")
        }
    }
}