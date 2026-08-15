package com.ng.pikop.feature.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.KnowledgeBaseArticle
import com.ng.pikop.ui.theme.PikopBlack
import com.ng.pikop.ui.theme.PikopOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportHubScreen(
    onNavigateToFaqList: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onBack: () -> Unit
) {
    var articles by remember { mutableStateOf<List<KnowledgeBaseArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            articles = apiService.getKnowledgeBase()
        } catch (_: Exception) {}
        isLoading = false
    }

    val categories = articles.map { it.category ?: "General" }.distinct()

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
                    containerColor = PikopBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = PikopBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Live Chat Card
            Card(
                onClick = onNavigateToChat,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PikopOrange)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = PikopBlack, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Support Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PikopBlack)
                        Text("Chat with a real agent now.", style = MaterialTheme.typography.bodySmall, color = PikopBlack.copy(alpha = 0.7f))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PikopBlack)
                }
            }

            Text(
                "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PikopOrange)
            } else if (categories.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No help articles found.", color = Color.Gray)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(categories) { category ->
                    CategoryItem(category) { onNavigateToFaqList(category) }
                }
            }
        }
    }
}

@Composable
fun CategoryItem(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.QuestionAnswer, contentDescription = null, tint = PikopOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
