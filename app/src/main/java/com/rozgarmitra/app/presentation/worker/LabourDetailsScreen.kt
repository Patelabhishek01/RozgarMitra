package com.rozgarmitra.app.presentation.worker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun LabourDetailsScreen() {

    var skills by remember {
        mutableStateOf("")
    }

    var experience by remember {
        mutableStateOf("")
    }

    var expectedWage by remember {
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
            text = "Labour Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Tell us about your work experience."
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        Text(
            text = "Skills",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = skills,
            onValueChange = {
                skills = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Example: Mason, Painter, Electrician")
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Experience",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = experience,
            onValueChange = {
                experience = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Example: 3 years")
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Expected Daily Wage",
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = expectedWage,
            onValueChange = {
                if (it.all { char -> char.isDigit() }) {
                    expectedWage = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Example: ₹600")
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
            enabled = skills.isNotBlank() &&
                    experience.isNotBlank() &&
                    expectedWage.isNotBlank()
        ) {
            Text("Complete Profile")
        }
    }
}