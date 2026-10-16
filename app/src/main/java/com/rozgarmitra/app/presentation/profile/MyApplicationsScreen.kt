package com.rozgarmitra.app.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.ApplicationStatus
import com.rozgarmitra.app.data.NotificationType
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBackClick: () -> Unit,
    onJobClick: (String) -> Unit
) {
    val applications by RozgarRepository.applications.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val notifications by RozgarRepository.notifications.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    
    val myApps = applications.filter { it.labourId == currentUser?.id }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                ),
                title = { Text("My Applications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (myApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(paddingValues), 
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSecondary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("You haven't applied to any jobs yet", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myApps, key = { it.id }) { app ->
                    val appNotifs = notifications.filter { notif ->
                        !notif.isRead && (
                            notif.relatedApplicationId == app.id ||
                            (notif.relatedJobId == app.jobId && notif.type != NotificationType.NEW_MESSAGE.name)
                        )
                    }
                    ApplicationCard(
                        title = app.jobTitle,
                        status = app.status,
                        hasUnreadNotification = appNotifs.isNotEmpty(),
                        onClick = {
                            if (appNotifs.isNotEmpty()) {
                                scope.launch {
                                    appNotifs.forEach { notif ->
                                        val res = RozgarRepository.markNotificationRead(notif.id)
                                        if (res.isFailure) {
                                            android.util.Log.e("MyApplicationsScreen", "Failed to mark notification read: ${notif.id}", res.exceptionOrNull())
                                        }
                                    }
                                }
                            }
                            onJobClick(app.jobId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationCard(
    title: String, 
    status: ApplicationStatus, 
    hasUnreadNotification: Boolean = false,
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        ApplicationStatus.APPLIED -> PrimaryBlue
        ApplicationStatus.ACCEPTED -> SuccessGreen
        ApplicationStatus.REJECTED -> WarningRose
        ApplicationStatus.COMPLETED -> AccentCyan
        ApplicationStatus.CANCELLED -> TextSecondary
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (hasUnreadNotification) PrimaryIndigo else BorderStrokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    if (hasUnreadNotification) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap to view job details", fontSize = 12.sp, color = TextSecondary)
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = status.name,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

