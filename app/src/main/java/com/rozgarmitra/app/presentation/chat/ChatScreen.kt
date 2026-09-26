package com.rozgarmitra.app.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
            topBar = {
                TopAppBar(
                    title = { Text("Unauthorized Access", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("You do not have permission to view this conversation.", color = Color.Red)
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Column {
                OfflineBanner(isOffline = isOffline)
                TopAppBar(
                    title = {
                        Column {
                            Text(thread?.otherUserName?.ifBlank { "Chat" } ?: "Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (relatedJob != null) {
                                Text("${relatedJob.title} • ₹${relatedJob.wage}/${relatedJob.wageType}", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                Text(
                                    text = if (isOffline) "offline" else "online",
                                    fontSize = 11.sp,
                                    color = if (isOffline) Color.Red else Color(0xFF2E7D32)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(26.dp))
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
        ) {

            // Chat history list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages yet.", color = Color.Gray)
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
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFECEFF1))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(msg.text, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                                
                                else -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                    ) {
                                        Surface(
                                            color = if (isMe) MaterialTheme.colorScheme.primary else Color.White,
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 16.dp
                                            ),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                when (msg.type) {
                                                    ChatMessageType.TEXT -> {
                                                        Text(
                                                            text = msg.text,
                                                            fontSize = 15.sp,
                                                            color = if (isMe) Color.White else Color.Black
                                                        )
                                                    }
                                                    ChatMessageType.LOCATION -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (isMe) Color.White else Color.Red)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Column {
                                                                Text("Shared Location", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isMe) Color.White else Color.Black)
                                                                Text(msg.text, fontSize = 12.sp, color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.DarkGray)
                                                            }
                                                        }
                                                    }
                                                    ChatMessageType.VOICE -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play voice", tint = if (isMe) Color.White else MaterialTheme.colorScheme.primary)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Voice Note (${msg.mediaDuration})", fontSize = 14.sp, color = if (isMe) Color.White else Color.Black)
                                                        }
                                                    }
                                                    ChatMessageType.IMAGE -> {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Filled.Image, contentDescription = null, tint = if (isMe) Color.White else Color.Gray)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Photo Attached", fontSize = 14.sp, color = if (isMe) Color.White else Color.Black)
                                                        }
                                                    }
                                                }
                                                
                                                Row(
                                                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                                        fontSize = 9.sp,
                                                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                                                    )
                                                    if (isMe) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White.copy(alpha = 0.7f))
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(chatError ?: "", color = Color(0xFFB71C1C), modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                    }
                }

                // Voice Recording Overlay Dialog
                if (isRecordingSim) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(160.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recording... (🎙️)", color = Color.White, fontWeight = FontWeight.Bold)
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
                                Text("Release to Send", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick replies chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                            label = { Text(phrase, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Divider()

            // Input Bar Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location share target (Large touch point!)
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Share Location", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }

                // Voice mic target (Large touch point!)
                IconButton(
                    onClick = { isRecordingSim = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Record Audio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }

                // Text field
                OutlinedTextField(
                    value = inputMsgText,
                    onValueChange = { inputMsgText = it },
                    placeholder = { Text("Type message...") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF9F9F9),
                        unfocusedContainerColor = Color(0xFFF9F9F9),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Send button (Large target!)
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputMsgText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.LightGray,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
