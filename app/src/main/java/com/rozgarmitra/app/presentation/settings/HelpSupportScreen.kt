package com.rozgarmitra.app.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozgarmitra.app.ui.theme.*

data class FaqItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val faqs = listOf(
        FaqItem(
            "How do I apply for a job?",
            "Browse active jobs on the Home screen or filter by radius/category. Tap on any job to view full details and click 'Apply Now'."
        ),
        FaqItem(
            "How do I chat with employers?",
            "Once an employer approves your application or approves chat, a secure chat thread opens automatically under the Messages tab."
        ),
        FaqItem(
            "How does location filtering work?",
            "The app uses GPS or your manually selected location to calculate distances to available jobs. Set the search radius slider to filter jobs near you."
        ),
        FaqItem(
            "How do I verify my profile?",
            "Go to your Profile screen and tap 'Verify Aadhaar' or 'Verify Business'. Submit your ID type for verification review."
        ),
        FaqItem(
            "How do job ratings work?",
            "After an employer marks a job as 'COMPLETED', both employer and worker can leave a rating and review for each other."
        )
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
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Contact Support",
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    fontSize = 14.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@rozgarmitra.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "RozgarMitra Support Request")
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, BorderStrokeColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Email Us", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("support@rozgarmitra.com", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:+9118001234567")
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, BorderStrokeColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Phone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Call Helpline", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("1800-123-4567", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Frequently Asked Questions",
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    fontSize = 14.sp
                )
            }

            items(faqs.indices.toList()) { index ->
                val item = faqs[index]
                val isExpanded = expandedFaqIndex == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFaqIndex = if (isExpanded) null else index },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, BorderStrokeColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                item.question,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(item.answer, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, BorderStrokeColor)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryIndigo)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About RozgarMitra", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("App version 1.0 • Terms & Privacy", fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            containerColor = DarkSurfaceElevated,
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About RozgarMitra", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("RozgarMitra v1.0", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "RozgarMitra connects local workers and employers seamlessly. Features include real-time job posting, radius filtering, direct messaging, application management, and rating system.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = AccentCyan)
                }
            }
        )
    }
}
