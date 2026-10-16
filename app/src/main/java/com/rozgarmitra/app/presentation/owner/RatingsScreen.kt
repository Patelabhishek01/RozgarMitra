package com.rozgarmitra.app.presentation.owner

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import com.rozgarmitra.app.R
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.LoadingOverlay
import com.rozgarmitra.app.presentation.components.PrimaryLargeButton
import com.rozgarmitra.app.presentation.components.RatingBar
import com.rozgarmitra.app.ui.theme.*
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
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text(stringResource(R.string.rate_worker), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back), modifier = Modifier.size(26.dp), tint = TextPrimary)
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
                        text = stringResource(R.string.job_completed),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen
                    )
                    Text(
                        text = stringResource(R.string.rate_parameters_desc),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Param 1: Skill
                    Text(stringResource(R.string.work_skill), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = skillRating, onRatingChanged = { skillRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (skillRating.toInt()) {
                                1 -> "★"
                                2 -> "★★"
                                3 -> "★★★"
                                4 -> "★★★★"
                                else -> "★★★★★"
                            },
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Param 2: Punctuality
                    Text(stringResource(R.string.punctuality), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = punctualityRating, onRatingChanged = { punctualityRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (punctualityRating.toInt()) {
                                1 -> "★"
                                2 -> "★★"
                                3 -> "★★★"
                                4 -> "★★★★"
                                else -> "★★★★★"
                            },
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Param 3: Behaviour
                    Text(stringResource(R.string.behaviour_conduct), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        RatingBar(rating = behaviourRating, onRatingChanged = { behaviourRating = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = when (behaviourRating.toInt()) {
                                1 -> "★"
                                2 -> "★★"
                                3 -> "★★★"
                                4 -> "★★★★"
                                else -> "★★★★★"
                            },
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Comment box
                    Text(stringResource(R.string.short_review_optional), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("...", color = TextSecondary.copy(alpha = 0.5f)) },
                        minLines = 3,
                        maxLines = 5,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                PrimaryLargeButton(
                    text = stringResource(R.string.submit_rating),
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            RozgarRepository.submitRating(
                                jobId = jobId,
                                targetUserId = "worker_1", 
                                stars = (skillRating + punctualityRating + behaviourRating) / 3f,
                                comment = commentText
                            ).collect { result ->
                                isLoading = false
                                result.fold(
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

            LoadingOverlay(isLoading = isLoading, text = stringResource(R.string.action_please_wait))
        }
    }
}

