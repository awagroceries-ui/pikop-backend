package com.ng.pikop.feature.order

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.ng.pikop.core.network.OrderDetailsResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersDashboardScreen(
    userEmail: String,
    onNewDelivery: () -> Unit,
    onTrackOrder: (String) -> Unit,
    onManageAddresses: () -> Unit,
    onGoToWallet: () -> Unit,
    onGoToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    var orders by remember { mutableStateOf<List<OrderDetailsResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            orders = apiService.getUserOrders()
        } catch (e: Exception) {
            // Error handled by Interceptor or locally
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.pikop_logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pikop", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            isLoading = true
                            try { orders = apiService.getUserOrders() } catch (e: Exception) {}
                            isLoading = false
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewDelivery,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Delivery")
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (orders.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.pikop_logo),
                            contentDescription = null,
                            modifier = Modifier.size(240.dp),
                            alpha = 0.8f
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No deliveries yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        Button(onClick = onNewDelivery, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Send something now")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            OrderCard(order, onTrackOrder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: OrderDetailsResponse, onTrack: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Order #${(order.id ?: "TBD").take(8)}", style = MaterialTheme.typography.titleMedium)
                StatusBadge(order.status ?: "UNKNOWN")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(text = "From: ${order.pickup_address ?: "N/A"}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Text(text = "To: ${order.delivery_address ?: "N/A"}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₦${order.total_fare ?: 0.0}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (order.status != "DELIVERED" && order.status != "CANCELLED") {
                    Button(
                        onClick = { onTrack(order.id ?: "") },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("Track")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "SEARCHING" -> Color(0xFFFFA000)
        "MATCHED" -> Color(0xFF1976D2)
        "PICKED_UP" -> Color(0xFF388E3C)
        "DELIVERED" -> Color.Gray
        "CANCELLED" -> Color.Red
        else -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
