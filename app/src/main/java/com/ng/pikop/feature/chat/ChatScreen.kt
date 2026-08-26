package com.ng.pikop.feature.chat

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ChatMessage
import com.ng.pikop.core.network.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String? = null,
    orderId: String? = null,
    userId: Int,
    userRole: String, // "CUSTOMER" or "FULFILLER"
    onBack: () -> Unit
) {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isTyping by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    val isSupport = conversationId != null

    fun refreshHistory() {
        isLoading = true
        scope.launch {
            try {
                val history = if (isSupport) {
                    apiService.getSupportMessages(conversationId!!)
                } else {
                    apiService.getOrderMessages(orderId!!)
                }
                messages.clear()
                messages.addAll(history)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    // Initial Pull
    LaunchedEffect(conversationId, orderId) {
        if (conversationId == null && orderId == null) return@LaunchedEffect
        refreshHistory()
    }

    // Real-time Socket Setup
    DisposableEffect(conversationId, orderId) {
        if (userId <= 0) return@DisposableEffect onDispose {}
        
        SocketManager.connect(userId.toString())
        
        SocketManager.on("connect") { 
            (context as? Activity)?.runOnUiThread { isConnected = true }
        }
        
        SocketManager.on("disconnect") { 
            (context as? Activity)?.runOnUiThread { isConnected = false }
        }

        SocketManager.on("receive_message") { data ->
            val newMsg = ChatMessage(
                id = data.optString("id", UUID.randomUUID().toString()),
                sender_id = data.optInt("sender_id", 0),
                sender_type = data.optString("sender_type", "USER"),
                content = data.optString("content", ""),
                text = data.optString("text", ""),
                created_at = data.optString("created_at", ""),
                is_read = data.optBoolean("is_read", false)
            )
            (context as? Activity)?.runOnUiThread {
                if (messages.none { it.id == newMsg.id }) {
                    messages.add(newMsg)
                }
            }
        }

        SocketManager.on("user_typing") { data ->
            if (data.optInt("userId") != userId) {
                (context as? Activity)?.runOnUiThread {
                    isTyping = data.optBoolean("isTyping")
                }
            }
        }

        onDispose {
            SocketManager.off("receive_message")
            SocketManager.off("user_typing")
            SocketManager.off("connect")
            SocketManager.off("disconnect")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isSupport) "Pikop Support" else "Mission Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isConnected) "Connected" else "Connecting...",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConnected) Color.Green else Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isConnected) Color.Green else Color.Red, CircleShape)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshHistory() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg, isMe = (msg.sender_id == userId || (userRole == "CUSTOMER" && msg.sender_type == "USER")))
                }
            }

            AnimatedVisibility(visible = isTyping) {
                Text(
                    "Agent is typing...",
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                    color = Color.Gray
                )
            }

            // Input Bar
            Surface(color = Color.White, tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding().imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...", color = Color.Gray) },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F0F0),
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isBlank()) return@FloatingActionButton
                            val content = inputText
                            inputText = ""
                            
                            val data = JSONObject().apply {
                                if (isSupport) put("conversation_id", conversationId)
                                else put("order_id", orderId)
                                
                                put("sender_id", userId)
                                put("sender_type", if (userRole == "CUSTOMER") "USER" else userRole)
                                put("content", content)
                            }
                            SocketManager.emit("send_message", data)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, isMe: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMe) 16.dp else 2.dp, bottomEnd = if (isMe) 2.dp else 16.dp),
            color = if (isMe) MaterialTheme.colorScheme.primary else Color.White,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(text = msg.messageText, style = MaterialTheme.typography.bodyMedium, color = if (isMe) Color.White else Color.Black)
                Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = msg.created_at?.takeLast(5) ?: "", style = MaterialTheme.typography.labelSmall, color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}
