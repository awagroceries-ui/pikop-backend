package com.ng.pikop.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ChatMessage
import com.ng.pikop.core.network.SocketManager
import com.ng.pikop.ui.theme.PikopBlack
import com.ng.pikop.ui.theme.PikopOrange
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String? = null,
    orderId: String? = null,
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

    val isSupport = conversationId != null

    // Fetch History
    LaunchedEffect(conversationId, orderId) {
        isLoading = true
        try {
            messages = if (isSupport) {
                apiService.getSupportMessages(conversationId!!)
            } else {
                apiService.getOrderMessages(orderId!!)
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    // Real-time Socket Setup
    DisposableEffect(conversationId, orderId) {
        SocketManager.connect()
        if (isSupport) {
            SocketManager.emit("join_support", JSONObject().put("conversationId", conversationId))
        } else {
            SocketManager.emit("join_order", JSONObject().put("orderId", orderId))
        }

        SocketManager.on("receive_message") { data ->
            val newMsg = ChatMessage(
                id = data.optString("id", "temp"),
                sender_id = data.optInt("senderId", 0),
                sender_type = data.optString("senderType", "USER"),
                body = data.optString("body", data.optString("content", "")),
                created_at = data.optString("created_at", "")
            )
            messages = messages + newMsg
        }

        onDispose {
            SocketManager.off("receive_message")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            if (isSupport) "Pikop Support" else "Order Chat",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!isSupport) {
                            Text("ID: #${orderId?.takeLast(8)}", style = MaterialTheme.typography.labelSmall, color = PikopOrange)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PikopBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = PikopBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isSupport) {
                Surface(color = PikopOrange.copy(alpha = 0.1f)) {
                    Text(
                        "Chat closes when the order is delivered",
                        style = MaterialTheme.typography.labelSmall,
                        color = PikopOrange,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg, isMe = (msg.sender_type == userRole || (userRole == "CUSTOMER" && msg.sender_type == "USER")))
                }
            }

            // Input Bar
            Surface(
                color = Color(0xFF1C1C1E),
                tonalElevation = 8.dp,
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
                        placeholder = { Text(if (isSupport) "Describe your issue..." else "Send a message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PikopOrange,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isBlank()) return@IconButton
                            val content = inputText
                            inputText = ""
                            
                            val data = JSONObject().apply {
                                if (isSupport) put("conversationId", conversationId)
                                else put("orderId", orderId)
                                
                                put("senderId", userId)
                                put("senderType", if (userRole == "CUSTOMER") "USER" else userRole)
                                put("content", content)
                            }
                            SocketManager.emit("send_message", data)
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = PikopOrange)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = PikopBlack)
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
            Text(
                "Pikop Official", 
                style = MaterialTheme.typography.labelSmall, 
                color = PikopOrange, 
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
        
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            ),
            color = if (isMe) PikopOrange else if (isAdmin) Color(0xFF2C2C2E) else Color(0xFF3A3A3C),
            tonalElevation = 2.dp
        ) {
            Text(
                text = msg.body ?: msg.content ?: "",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = if (isMe) PikopBlack else Color.White
            )
        }
        
        msg.created_at?.let {
            Text(
                text = it.takeLast(5), // Assume "HH:mm" suffix or similar
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}
