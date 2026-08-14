package com.rozgarmitra.app.presentation.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OwnerDetailsScreen() {

    var address by remember {
        mutableStateOf("")
    }

    var companyName by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {

        Text(
            text = "Owner Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Tell us about your business or work location."
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Address",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Enter your address")
            },
            minLines = 3
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Company Name (Optional)",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = companyName,
            onValueChange = {
                companyName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Enter company name")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Button(
            onClick = {
                // We'll save the profile later.
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = address.isNotBlank()
        ) {
            Text("Complete Profile")
        }
    }
}