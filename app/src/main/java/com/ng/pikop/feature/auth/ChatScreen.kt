package com.ng.pikop.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ChatMessage
import com.ng.pikop.core.network.SocketManager
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    userId: Int,
    userRole: String, // "CUSTOMER" or "FULFILLER"
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    // Fetch History
    LaunchedEffect(conversationId) {
        isLoading = true
        try {
            messages = apiService.getSupportMessages(conversationId)
        } catch (_: Exception) {}
        isLoading = false
    }

    // Real-time Socket Setup
    DisposableEffect(conversationId) {
        SocketManager.connect()
        SocketManager.emit("join_support", JSONObject().put("conversationId", conversationId))

        SocketManager.on("new_message") { data ->
            val newMsg = ChatMessage(
                id = data.optString("id", "temp"),
                sender_id = data.optInt("senderId", 0),
                sender_type = data.optString("senderType", "USER"),
                content = data.optString("content", ""),
                created_at = data.optString("created_at", "")
            )
            messages = messages + newMsg
        }

        onDispose {
            SocketManager.off("new_message")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pikop Support") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg, isMe = msg.sender_type == userRole)
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Describe your issue...") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isBlank()) return@IconButton
                            val content = inputText
                            inputText = ""
                            
                            val data = JSONObject().apply {
                                put("conversationId", conversationId)
                                put("senderId", userId)
                                put("senderType", userRole)
                                put("content", content)
                            }
                            SocketManager.emit("send_message", data)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    val isAdmin = msg.sender_type == "ADMIN"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (isAdmin) {
            Text("Official Support", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 4.dp))
        }
        
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMe) MaterialTheme.colorScheme.primary else if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Text(
                text = msg.content ?: "",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
