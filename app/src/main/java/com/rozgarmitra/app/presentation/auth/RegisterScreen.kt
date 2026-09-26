package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    identifier: String, // Can be phone or email
    onLabourRegistered: () -> Unit,
    onOwnerRegistered: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } 
    val isEmail = identifier.contains("@") || identifier == "email_user"
    
    // Determine if we need to show password field (only for new Email accounts, not Google)
    val currentUser = RozgarRepository.currentUser.collectAsState().value
    val needsPassword = isEmail && FirebaseAuth.getInstance().currentUser == null
    
    var selectedRole by remember { mutableStateOf<Role?>(null) }
    var selectedLanguage by remember { mutableStateOf("English") }
    var expandedLangDropdown by remember { mutableStateOf(false) }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Profile", fontWeight = FontWeight.Bold) }
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
                        text = "Register with RozgarMitra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter details to create your secure account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Full Name Input
                    Text(
                        text = "Full Name / पूरा नाम",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Identifier Field
                    Text(
                        text = if (isEmail) "Email Address" else "Mobile Number (Verified)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (isEmail && identifier == "email_user") "" else if (isEmail) identifier else "+91 $identifier",
                        onValueChange = {},
                        enabled = isEmail && identifier == "email_user", // Only allow edit if it's a new email user
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    if (needsPassword) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Create Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            placeholder = { Text("Minimum 6 characters") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Role Card Selection (Huge Targets!)
                    Text(
                        text = "Choose Account Type / खाता प्रकार चुनें",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Labour Card
                        val isLabourSelected = selectedRole == Role.LABOUR
                        Card(
                            onClick = { selectedRole = Role.LABOUR },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLabourSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isLabourSelected) 2.dp else 1.dp,
                                color = if (isLabourSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Engineering,
                                    contentDescription = null,
                                    tint = if (isLabourSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Labour\n(कामगार)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (isLabourSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Owner Card
                        val isOwnerSelected = selectedRole == Role.OWNER
                        Card(
                            onClick = { selectedRole = Role.OWNER },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOwnerSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isOwnerSelected) 2.dp else 1.dp,
                                color = if (isOwnerSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Business,
                                    contentDescription = null,
                                    tint = if (isOwnerSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Employer / Owner\n(मालिक)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = if (isOwnerSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Language Selector Dropdown
                    Text(
                        text = "App Language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { expandedLangDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedLanguage,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Expand"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedLangDropdown,
                            onDismissRequest = { expandedLangDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            languagesList.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.nativeName} (${lang.englishName})") },
                                    onClick = {
                                        selectedLanguage = lang.englishName
                                        expandedLangDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                PrimaryLargeButton(
                    text = "Register Profile",
                    onClick = {
                        val role = selectedRole ?: return@PrimaryLargeButton
                        isLoading = true
                        errorMessage = null
                        
                        val flow = if (needsPassword) {
                            RozgarRepository.registerWithEmail(identifier, password, fullName, role, selectedLanguage)
                        } else {
                            // Already authenticated via Google or Phone
                            RozgarRepository.register(fullName, identifier, role, selectedLanguage)
                        }
                        
                        scope.launch {
                            try {
                                flow.collect { result ->
                                    result.fold(
                                        onSuccess = {
                                            // Ensure loading is cleared before navigating
                                            delay(500)
                                            isLoading = false
                                            if (role == Role.LABOUR) onLabourRegistered()
                                            else onOwnerRegistered()
                                        },
                                        onFailure = { error ->
                                            isLoading = false
                                            errorMessage = error.localizedMessage ?: "Registration failed."
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Unexpected error: ${e.localizedMessage}"
                            }
                        }
                    },
                    enabled = fullName.isNotBlank() && selectedRole != null
                )
            }

            LoadingOverlay(isLoading = isLoading, text = "Registering Account...")
        }
    }
}