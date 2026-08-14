package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Star
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
import com.ng.pikop.core.network.FulfillerOrderResponse
import com.ng.pikop.ui.theme.PikopBlack
import com.ng.pikop.ui.theme.PikopOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(onBack: () -> Unit) {
    var history by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var rating by remember { mutableStateOf(5.0) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            history = apiService.getFulfillerOrders()
            val profile = apiService.getFulfillerProfile()
            rating = profile.rating_avg ?: 5.0
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = PikopBlack
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightCard(
                    title = "Total Earnings",
                    value = "₦${"%,.0f".format(history.sumOf { it.earnings ?: 0.0 })}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                InsightCard(
                    title = "Rating",
                    value = rating.toString(),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Mission History", style = MaterialTheme.typography.titleMedium, color = PikopOrange)
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PikopOrange)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { order ->
                    MissionInsightItem(order)
                }
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = PikopOrange, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun MissionInsightItem(order: FulfillerOrderResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Order #${order.id}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(order.created_at?.take(10) ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₦${"%,.2f".format(order.earnings ?: 0.0)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = PikopOrange)
                Text(order.status ?: "COMPLETED", style = MaterialTheme.typography.labelSmall, color = if (order.status == "DELIVERED") Color.Green else Color.LightGray)
            }
        }
    }
}
