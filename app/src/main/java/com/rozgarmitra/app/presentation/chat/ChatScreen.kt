package com.rozgarmitra.app.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rozgarmitra.app.data.ChatMessageType
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.components.OfflineBanner
import com.rozgarmitra.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val quickReplies = listOf(
    "I am on my way (मैं आ रहा हूँ)",
    "What is the location? (लोकेशन क्या है?)",
    "Call me (मुझे फ़ोन करो)",
    "Understood (समझ गया)",
    "Ready to start tomorrow"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    threadId: String,
    onBackClick: () -> Unit
) {
    val threads by RozgarRepository.threads.collectAsStateWithLifecycle()
    val allMessages by RozgarRepository.messages.collectAsStateWithLifecycle()
    val isOffline by RozgarRepository.isOffline.collectAsStateWithLifecycle()
    val currentUser by RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val jobs by RozgarRepository.jobs.collectAsStateWithLifecycle()

    val thread = threads.firstOrNull { it.id == threadId }
    val messages = allMessages[threadId] ?: emptyList()
    val relatedJob = jobs.firstOrNull { it.id == thread?.jobId }

    var inputMsgText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }

    // Voice note simulation state
    var isRecordingSim by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Observe messages for threadId
    LaunchedEffect(threadId) {
        RozgarRepository.observeMessages(threadId)
    }

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(chatError) {
        chatError?.let {
            delay(3000)
            chatError = null
        }
    }

    // Security check: Authorization verification
    val isAuthorized = currentUser != null && (thread == null || thread.participants.isEmpty() || currentUser!!.id in thread.participants || thread.ownerId == currentUser!!.id || thread.workerId == currentUser!!.id)

    if (!isAuthorized) {
        Scaffold(
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                    title = { Text("Unauthorized Access", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(DarkBackground), contentAlignment = Alignment.Center) {
                Text("You do not have permission to view this conversation.", color = WarningRose)
            }
        }
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column {
                OfflineBanner(isOffline = isOffline)
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                    title = {
                        Column {
                            Text(thread?.getOtherUserName(currentUser?.id ?: "")?.ifBlank { "Chat" } ?: "Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            if (relatedJob != null) {
                                Text("${relatedJob.title} • ₹${relatedJob.wage}/${relatedJob.wageType}", fontSize = 11.sp, color = TextSecondary)
                            } else {
                                Text(
                                    text = if (isOffline) "offline" else "online",
                                    fontSize = 11.sp,
                                    color = if (isOffline) WarningRose else SuccessGreen
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(24.dp))
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {

            // Chat history list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBackground)
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages yet.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { msg ->
                            val isMe = msg.senderId == currentUser?.id
                            val isSystem = msg.senderId == "system"

                            when {
                                isSystem -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = DarkSurface,
                                            border = BorderStroke(1.dp, BorderStrokeColor),
                                            shape = CircleShape,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(msg.text, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                                
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                    ) {
                                        Surface(
                                            color = if (isMe) PrimaryIndigo else DarkSurface,
                                            border = if (isMe) null else BorderStroke(1.dp, BorderStrokeColor),
                                            shape = RoundedCornerShape(
                                                topStart = 18.dp,
                                                topEnd = 18.dp,
                                                bottomStart = if (isMe) 18.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 18.dp
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                                when (msg.type) {
                                                    ChatMessageType.TEXT -> {
                                                        Text(
                                                            text = msg.text,
                                                            fontSize = 15.sp,
                                                            color = if (isMe) Color.White else TextPrimary
                                                        )
                                                    }
                                                    ChatMessageType.LOCATION -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (isMe) Color.White else AccentCyan)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Column {
                                                                Text("Shared Location", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isMe) Color.White else TextPrimary)
                                                                Text(msg.text, fontSize = 12.sp, color = if (isMe) Color.White.copy(alpha = 0.8f) else TextSecondary)
                                                            }
                                                        }
                                                    }
                                                    ChatMessageType.VOICE -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice", tint = if (isMe) Color.White else PrimaryIndigo)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Voice Note (${msg.mediaDuration})", fontSize = 14.sp, color = if (isMe) Color.White else TextPrimary)
                                                        }
                                                    }
                                                    ChatMessageType.IMAGE -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.Image, contentDescription = null, tint = if (isMe) Color.White else TextSecondary)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Photo Attached", fontSize = 14.sp, color = if (isMe) Color.White else TextPrimary)
                                                        }
                                                    }
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                                        fontSize = 9.sp,
                                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else TextMuted
                                                    )
                                                    if (isMe) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White.copy(alpha = 0.8f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Error overlay banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = chatError != null,
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarningRose.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, WarningRose.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(chatError ?: "", color = WarningRose, modifier = Modifier.padding(8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Voice Recording Overlay Dialog
                if (isRecordingSim) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(160.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, BorderStrokeColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = WarningRose, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recording... 🎙️", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    isRecordingSim = false
                                    // Send simulated voice message
                                    scope.launch {
                                        RozgarRepository.sendMessage(
                                            threadId = threadId,
                                            text = "Simulated Voice Note",
                                            type = ChatMessageType.VOICE,
                                            mediaDuration = "0:06"
                                        ).collect { result ->
                                            result.onFailure { err -> chatError = err.localizedMessage }
                                        }
                                    }
                                }
                            ) {
                                Text("Release to Send", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick replies chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickReplies.forEach { phrase ->
                        SuggestionChip(
                            onClick = {
                                inputMsgText = phrase.substringBefore(" (")
                            },
                            label = { Text(phrase, fontSize = 11.sp, color = TextPrimary) },
                            shape = CircleShape,
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = DarkBackground),
                            border = SuggestionChipDefaults.suggestionChipBorder(borderColor = BorderStrokeColor)
                        )
                    }
                }
            }

            Divider(color = BorderStrokeColor.copy(alpha = 0.6f))

            // Input Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            RozgarRepository.sendMessage(
                                threadId = threadId,
                                text = "12.9716° N, 77.5946° E (Sharma Site)",
                                type = ChatMessageType.LOCATION
                            ).collect { result ->
                                result.onFailure { err -> chatError = err.localizedMessage }
                            }
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Share Location", tint = AccentCyan, modifier = Modifier.size(24.dp))
                }

                IconButton(
                    onClick = { isRecordingSim = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Record Audio", tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
                }

                // Text field
                OutlinedTextField(
                    value = inputMsgText,
                    onValueChange = { inputMsgText = it },
                    placeholder = { Text("Type message...", color = TextMuted) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = DarkBackground,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderStrokeColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputMsgText.isNotBlank()) {
                            val textToSend = inputMsgText
                            inputMsgText = ""
                            scope.launch {
                                RozgarRepository.sendMessage(
                                    threadId = threadId,
                                    text = textToSend,
                                    type = ChatMessageType.TEXT
                                ).collect { result ->
                                    result.onFailure { err ->
                                        inputMsgText = textToSend
                                        chatError = err.localizedMessage
                                    }
                                }
                            }
                        }
                    },
                    enabled = inputMsgText.isNotBlank(),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputMsgText.isNotBlank()) PrimaryIndigo else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

