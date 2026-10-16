package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.ui.theme.*
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

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResetLoading by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }
    var resetSuccess by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Google Sign-In Setup
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("665387151392-bra33spjvo6s581tc5nn1bsd9hg325i4.apps.googleusercontent.com")
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        fontSize = 36.sp
                    )

                    Text(
                        text = stringResource(R.string.connecting_hands),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
                    )

                    Text(
                        text = if (isRegistering) stringResource(R.string.create_account_title) else stringResource(R.string.welcome_back),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = if (isRegistering) stringResource(R.string.enter_details_start) else stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
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
                            label = { Text(stringResource(R.string.email_address)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryBlue)
                            },
                            singleLine = true,
                            colors = textFieldColors,
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
                            label = { Text(stringResource(R.string.password)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = PrimaryBlue)
                            },
                            singleLine = true,
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = MaterialTheme.shapes.medium
                        )

                        if (!isRegistering) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        resetEmail = email
                                        resetError = null
                                        resetSuccess = null
                                        showResetDialog = true
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.forgot_password),
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
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
                            label = { Text(stringResource(R.string.mobile_number)) },
                            placeholder = { Text(stringResource(R.string.mobile_number_hint), color = TextSecondary.copy(alpha = 0.5f)) },
                            prefix = {
                                Text(
                                    text = "+91 ",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            },
                            singleLine = true,
                            colors = textFieldColors,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Footer Content (Buttons)
                Column(modifier = Modifier.fillMaxWidth()) {
                    PrimaryLargeButton(
                        text = when {
                            isRegistering -> stringResource(R.string.continue_btn)
                            isEmailLogin -> stringResource(R.string.action_login)
                            else -> stringResource(R.string.send_otp)
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
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = DarkSurface,
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeColor)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = AccentCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.continue_with_google), color = TextPrimary, fontWeight = FontWeight.Bold)
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
                                if (isRegistering) stringResource(R.string.already_have_account) 
                                else stringResource(R.string.dont_have_account),
                                color = AccentCyan,
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
                                if (isEmailLogin) stringResource(R.string.use_phone_login) 
                                else stringResource(R.string.use_email_login),
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.select_language_title),
                            modifier = Modifier
                                .clickable { onLanguageSelectClick() }
                                .padding(8.dp),
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            LoadingOverlay(
                isLoading = isLoading, 
                text = stringResource(R.string.action_please_wait)
            )

            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { 
                        if (!isResetLoading) showResetDialog = false 
                    },
                    containerColor = DarkSurface,
                    title = {
                        Text(
                            text = stringResource(R.string.reset_password_title),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = stringResource(R.string.reset_password_desc),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.email_address)) },
                                singleLine = true,
                                colors = textFieldColors
                            )
                            if (resetError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resetError!!,
                                    color = WarningRose,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (resetSuccess != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resetSuccess!!,
                                    color = SuccessGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                isResetLoading = true
                                resetError = null
                                resetSuccess = null
                                scope.launch {
                                    RozgarRepository.sendPasswordResetEmail(resetEmail).collect { res ->
                                        isResetLoading = false
                                        res.fold(
                                            onSuccess = {
                                                resetSuccess = "Reset link sent! Please check your email inbox and spam folder."
                                            },
                                            onFailure = { err ->
                                                resetError = err.localizedMessage ?: "Failed to send reset link."
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isResetLoading && resetEmail.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            if (isResetLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.send_reset_link), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showResetDialog = false },
                            enabled = !isResetLoading
                        ) {
                            Text(stringResource(R.string.action_cancel), color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}