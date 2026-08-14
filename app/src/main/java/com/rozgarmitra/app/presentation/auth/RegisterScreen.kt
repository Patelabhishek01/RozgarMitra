package com.rozgarmitra.app.presentation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    onLabourSelected: () -> Unit,
    onOwnerSelected: () -> Unit
) {

    var fullName by remember {
        mutableStateOf("")
    }

    var mobileNumber by remember {
        mutableStateOf("")
    }

    var selectedRole by remember {
        mutableStateOf("")
    }

    var selectedLanguage by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Join RozgarMitra and get started."
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // Full Name

        Text(
            text = "Full Name",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Enter your full name")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Mobile Number

        Text(
            text = "Mobile Number",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = mobileNumber,
            onValueChange = {
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    mobileNumber = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Enter mobile number")
            },
            prefix = {
                Text("+91 ")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // User Type

        Text(
            text = "I am a",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        RoleOption(
            text = "Labour",
            selected = selectedRole == "Labour",
            onClick = {
                selectedRole = "Labour"
            }
        )

        RoleOption(
            text = "Owner",
            selected = selectedRole == "Owner",
            onClick = {
                selectedRole = "Owner"
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Preferred Language

        Text(
            text = "Preferred Language",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = selectedLanguage,
            onValueChange = {
                selectedLanguage = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Example: Hindi")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Button(
            onClick = {

                if (selectedRole == "Labour") {
                    onLabourSelected()
                } else if (selectedRole == "Owner") {
                    onOwnerSelected()
                }

            },
            modifier = Modifier.fillMaxWidth(),
            enabled = fullName.isNotBlank() &&
                    mobileNumber.length == 10 &&
                    selectedRole.isNotBlank() &&
                    selectedLanguage.isNotBlank()
        ) {
            Text("Continue")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}

@Composable
private fun RoleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(text = text)
    }
}