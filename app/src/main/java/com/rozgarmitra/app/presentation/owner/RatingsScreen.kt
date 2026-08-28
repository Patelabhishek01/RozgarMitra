package com.rozgarmitra.app.presentation.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
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
import com.rozgarmitra.app.presentation.components.RatingBar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingsScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    var skillRating by remember { mutableStateOf(4f) }
    var punctualityRating by remember { mutableStateOf(4f) }
    var behaviourRating by remember { mutableStateOf(4f) }
    var commentText by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Worker", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(26.dp))
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Job Completed! 🎉",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Please rate the worker on the following parameters.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Param 1: Skill
                    Text("Work Skill / कार्य कौशल", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = skillRating, onRatingChanged = { skillRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (skillRating.toInt()) {
                                1 -> "Poor"
                                2 -> "Average"
                                3 -> "Good"
                                4 -> "Very Good"
                                else -> "Excellent"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Param 2: Punctuality
                    Text("Punctuality / समय पालन", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = punctualityRating, onRatingChanged = { punctualityRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (punctualityRating.toInt()) {
                                1 -> "Very Late"
                                2 -> "Late"
                                3 -> "On Time"
                                4 -> "Punctual"
                                else -> "Highly Reliable"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Param 3: Behaviour
                    Text("Behaviour & Conduct / व्यवहार", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = behaviourRating, onRatingChanged = { behaviourRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (behaviourRating.toInt()) {
                                1 -> "Rude"
                                2 -> "Polite"
                                3 -> "Helpful"
                                4 -> "Very Helpful"
                                else -> "Exceptional"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Comment box
                    Text("Write a short review (Optional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Completed work quickly, did a great job") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryLargeButton(
                    text = "Submit Rating",
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        val flow = RozgarRepository.submitRating(
                            jobId = jobId,
                            targetUserId = "worker_1", // Hired worker placeholder
                            stars = (skillRating + punctualityRating + behaviourRating) / 3f,
                            comment = commentText
                        )
                        scope.launch {
                            flow.collectLatest { res ->
                                isLoading = false
                                res.fold(
                                    onSuccess = {
                                        RozgarRepository.addNotification("Rating Submitted", "Thank you for reviewing the worker.")
                                        onSubmitSuccess()
                                    },
                                    onFailure = { err -> errorMessage = err.localizedMessage }
                                )
                            }
                        }
                    }
                )
            }

            LoadingOverlay(isLoading = isLoading, text = "Submitting Review...")
        }
    }
}
