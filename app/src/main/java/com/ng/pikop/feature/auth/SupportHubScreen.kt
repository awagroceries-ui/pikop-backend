package com.ng.pikop.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.KnowledgeBaseArticle
import com.ng.pikop.ui.theme.PikopOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    userRole: String,
    onNavigateToFaqList: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onBack: () -> Unit
) {
    var articles by remember { mutableStateOf<List<KnowledgeBaseArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeGroup by remember { mutableStateOf(if (userRole == "MERCHANT") "MERCHANT" else if (userRole == "FULFILLER") "FULFILLER" else "CUSTOMER") }
    
    // AI Assistant State
    var showAiChat by remember { mutableStateOf(false) }
    var aiQuestion by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf<String?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    fun fetchArticles() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                articles = apiService.getKnowledgeBase(activeGroup)
            } catch (e: Exception) {
                errorMessage = "Failed to load help articles. Check your connection."
            }
            isLoading = false
        }
    }

    LaunchedEffect(activeGroup) {
        fetchArticles()
    }

    // Filter logic
    val filteredArticles = articles.filter {
        it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true)
    }
    val categories = filteredArticles.map { it.category ?: "General" }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Role Switcher (Multi-role support)
            // Note: In Pikop V3, users often have one primary role, but we support switching if they are a MERCHANT/FULFILLER too.
            if (userRole == "MERCHANT" || userRole == "FULFILLER") {
                TabRow(
                    selectedTabIndex = if (activeGroup == "CUSTOMER") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeGroup == "CUSTOMER",
                        onClick = { activeGroup = "CUSTOMER" },
                        text = { Text("Customer Help") }
                    )
                    Tab(
                        selected = activeGroup == userRole,
                        onClick = { activeGroup = userRole },
                        text = { Text(if (userRole == "MERCHANT") "Seller Help" else "Agent Help") }
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search help articles...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) } }
                } else null,
                shape = RoundedCornerShape(12.dp)
            )

            // Pikop Agent AI Card (v4.7)
            Card(
                onClick = { showAiChat = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PikopOrange.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = PikopOrange)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Ask Pikop Agent", fontWeight = FontWeight.Bold, color = PikopOrange)
                        Text("Instant answers powered by AI.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            // Live Chat Card
            Card(
                onClick = onNavigateToChat,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSecondary, 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Live Support Chat", 
                            style = MaterialTheme.typography.titleSmall, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            "Chat with a real agent now.", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                "Common Questions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PikopOrange)
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage!!, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { fetchArticles() }) {
                        Text("Retry")
                    }
                }
            } else if (categories.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "No help articles found." else "No results for '$searchQuery'", color = Color.Gray)
                }
            }

            val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(categories) { category ->
                    val categoryArticles = filteredArticles.filter { it.category == category }
                    val expanded = expandedCategories[category] ?: false

                    CategoryAccordion(
                        name = category,
                        count = categoryArticles.size,
                        isExpanded = expanded,
                        onToggle = { expandedCategories[category] = !expanded }
                    )

                    if (expanded) {
                        categoryArticles.forEach { article ->
                            FAQListItem(article.title) { onNavigateToFaqList(article.id) }
                        }
                    }
                }
            }
        }
    }

    if (showAiChat) {
        AlertDialog(
            onDismissRequest = { 
                showAiChat = false
                aiQuestion = ""
                aiAnswer = null
            },
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = PikopOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pikop AI Agent")
            }},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (aiAnswer != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                aiAnswer!!, 
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedTextField(
                        value = aiQuestion,
                        onValueChange = { aiQuestion = it },
                        label = { Text("How can I help you?") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (isAiThinking) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            isAiThinking = true
                                            try {
                                                val res = apiService.askAiAssistant(mapOf("question" to aiQuestion))
                                                aiAnswer = res["answer"]?.toString() ?: "I couldn't find an answer. Try rephrasing or chat with support."
                                            } catch (e: Exception) {
                                                aiAnswer = "Error connecting to AI. Please try again later."
                                            }
                                            isAiThinking = false
                                        }
                                    },
                                    enabled = aiQuestion.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Send, null, tint = PikopOrange)
                                }
                            }
                        }
                    )
                    
                    if (aiAnswer != null) {
                        TextButton(
                            onClick = onNavigateToChat,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Still need help? Chat with a human", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showAiChat = false
                    aiQuestion = ""
                    aiAnswer = null
                }) { Text("Close") }
            }
        )
    }
}

@Composable
fun CategoryAccordion(name: String, count: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            if (count > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Text("$count", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun FAQListItem(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
