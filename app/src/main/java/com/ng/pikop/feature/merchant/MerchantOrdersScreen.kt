package com.ng.pikop.feature.merchant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.OrderDetailsResponse
import kotlinx.coroutines.launch

@Composable
fun MerchantOrdersScreen(onNavigateToOrderDetails: (String) -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var orders by remember { mutableStateOf<List<OrderDetailsResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val fetchOrders = {
        scope.launch {
            isLoading = true
            try {
                // We'll create this API method next
                orders = apiService.getMerchantIncomingOrders()
            } catch (e: Exception) {
                // Ignore for now
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchOrders()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Incoming Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No incoming orders.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToOrderDetails(order.id ?: "") }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Order #${order.id?.takeLast(6)}", fontWeight = FontWeight.Bold)
                            Text("Status: ${order.status}")
                            Text("Item: ${order.item_description ?: "Products"}")
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (order.status == "PAYMENT_CAPTURED" || order.status == "SEARCHING") {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                apiService.updateMerchantOrderStatus(order.id ?: "", mapOf("status" to "PREPARING"))
                                                fetchOrders()
                                            } catch (e: Exception) {}
                                        }
                                    }
                                ) {
                                    Text("Accept & Prepare")
                                }
                            } else if (order.status == "PREPARING") {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                apiService.updateMerchantOrderStatus(order.id ?: "", mapOf("status" to "READY_FOR_PICKUP"))
                                                fetchOrders()
                                            } catch (e: Exception) {}
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Mark Ready for Pickup")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
