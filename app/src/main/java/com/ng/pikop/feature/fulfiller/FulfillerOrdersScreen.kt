package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.FulfillerOrderResponse
import com.ng.pikop.feature.order.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillerOrdersScreen(onBack: () -> Unit) {
    var orders by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            orders = apiService.getFulfillerOrders()
        } catch (e: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (orders.isEmpty()) {
                Text("No deliveries completed yet.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Total Earnings Header
                    val totalEarnings = orders.sumOf { it.earnings ?: 0.0 }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.pikop_badge),
                                contentDescription = null,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Lifetime Earnings (75%)", style = MaterialTheme.typography.labelSmall)
                                Text("₦${"%,.2f".format(totalEarnings)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            FulfillerOrderCard(order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FulfillerOrderCard(order: FulfillerOrderResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Order #${order.id ?: 0}", style = MaterialTheme.typography.titleMedium)
                StatusBadge(order.status ?: "UNKNOWN")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(text = "Pickup: ${order.pickup_address ?: "N/A"}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
            Text(text = "Dropoff: ${order.delivery_address ?: "N/A"}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Earning", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("₦${"%.2f".format(order.earnings ?: 0.0)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                }
                
                Text(
                    text = (order.created_at ?: "").take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}
