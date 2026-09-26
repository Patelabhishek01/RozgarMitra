package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountCircle
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSendOtpSuccess: (String) -> Unit,
    onLoginSuccess: (Boolean) -> Unit,
    onNavigateToRegister: (String, String?) -> Unit, // identifier, password
    onLanguageSelectClick: () -> Unit
) {
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEmailLogin by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Google Sign-In Setup
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("665387151392-bra33spjvo6s581tc5nn1bsd9hg325i4.apps.googleusercontent.com") // User needs to replace this
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                isLoading = true
                val flow = RozgarRepository.loginWithGoogle(idToken)
                scope.launch {
                    flow.collectLatest { res ->
                        isLoading = false
                        res.fold(
                            onSuccess = { user ->
                                if (user != null) {
                                    RozgarRepository.executePendingAction()
                                    onLoginSuccess(user.profileCompleted)
                                } else onNavigateToRegister(account.email ?: "google_user", null)
                            },
                            onFailure = { e -> errorMessage = e.localizedMessage }
                        )
                    }
                }
            }
        } catch (e: ApiException) {
            errorMessage = "Google Sign-In failed: " + e.localizedMessage
        }
    }

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
                        text = if (isRegistering) "Create your account" else "Welcome / स्वागत है!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isRegistering) "Enter details to start your journey." else "Enter your ${if (isEmailLogin) "email" else "mobile number"} to login or register.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    if (isEmailLogin) {
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            label = { Text("Email Address") },
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = MaterialTheme.shapes.medium
                        )
                    } else {
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
                    }

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
                        text = when {
                            isRegistering -> "Continue / आगे बढ़ें"
                            isEmailLogin -> "Login / Login"
                            else -> "Send OTP / ओटीपी भेजें"
                        },
                        onClick = {
                            if (isRegistering) {
                                if (isEmailLogin) onNavigateToRegister(email, password)
                                else {
                                    isLoading = true
                                    val flow = RozgarRepository.login(context as Activity, mobileNumber)
                                    scope.launch {
                                        flow.collect { result ->
                                            isLoading = false
                                            result.fold(
                                                onSuccess = { onSendOtpSuccess(mobileNumber) },
                                                onFailure = { error -> errorMessage = error.localizedMessage }
                                            )
                                        }
                                    }
                                }
                                return@PrimaryLargeButton
                            }

                            isLoading = true
                            errorMessage = null
                            
                            if (isEmailLogin) {
                                val flow = RozgarRepository.loginWithEmail(email, password)
                                scope.launch {
                                    flow.collect { result ->
                                        // Wait a bit to ensure Firestore syncs profile before callback
                                        delay(1000)
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { user ->
                                                if (user != null) {
                                                    RozgarRepository.executePendingAction()
                                                    onLoginSuccess(user.profileCompleted)
                                                } else errorMessage = "Profile not found. Please register."
                                            },
                                            onFailure = { error ->
                                                errorMessage = error.localizedMessage ?: "Login failed."
                                            }
                                        )
                                    }
                                }
                            } else {
                                val flow = RozgarRepository.login(context as Activity, mobileNumber)
                                scope.launch {
                                    flow.collect { result ->
                                        isLoading = false
                                        result.fold(
                                            onSuccess = { onSendOtpSuccess(mobileNumber) },
                                            onFailure = { error ->
                                                errorMessage = error.localizedMessage ?: "OTP failed."
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        enabled = if (isEmailLogin) email.isNotEmpty() && password.isNotEmpty() else mobileNumber.length == 10
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isRegistering) {
                        // Google Sign In
                        OutlinedButton(
                            onClick = {
                                googleLauncher.launch(googleSignInClient.signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Sign in with Google", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { isRegistering = !isRegistering }) {
                            Text(
                                if (isRegistering) "Already have an account? Login" 
                                else "Don't have an account? Register",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { isEmailLogin = !isEmailLogin }) {
                            Text(
                                if (isEmailLogin) "Use Phone Number / मोबाइल का उपयोग करें" 
                                else "Use Email / ईमेल का उपयोग करें",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
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
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LoadingOverlay(
                isLoading = isLoading, 
                text = if (isEmailLogin) "Authenticating..." else "Sending OTP..."
            )
        }
    }
}