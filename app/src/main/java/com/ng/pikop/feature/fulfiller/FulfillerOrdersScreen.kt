package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.FulfillerOrderResponse
import com.ng.pikop.feature.order.StatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillerOrdersScreen(onBack: () -> Unit, onNavigateToActiveOrder: (String) -> Unit) {
    var orders by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    val fetchOrders = {
        scope.launch {
            isLoading = true
            try {
                orders = apiService.getFulfillerOrders()
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                fetchOrders()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        fetchOrders()
    }

    val activeOrders = orders.filter { order ->
        val s = order.status?.uppercase() ?: ""
        s.isNotBlank() && s != "QUEUED" && s !in listOf("DELIVERED", "CANCELLED", "RELEASED", "REFUNDED", "RECIPIENT_ABSENT")
    }

    val queuedOrders = orders.filter { order ->
        val s = order.status?.uppercase() ?: ""
        s == "QUEUED"
    }

    val completedOrders = orders.filter { order ->
        val s = order.status?.uppercase() ?: ""
        s in listOf("DELIVERED", "RELEASED")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mission Records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Active (${activeOrders.size})") },
                    icon = { Icon(Icons.Default.DirectionsBike, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Queued (${queuedOrders.size})") },
                    icon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Completed (${completedOrders.size})") },
                    icon = { Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    val currentList = when (selectedTabIndex) {
                        0 -> activeOrders
                        1 -> queuedOrders
                        else -> completedOrders
                    }

                    if (currentList.isEmpty()) {
                        val emptyMsg = when (selectedTabIndex) {
                            0 -> "No active missions right now."
                            1 -> "No queued missions waiting."
                            else -> "No completed deliveries yet."
                        }
                        Text(emptyMsg, modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (selectedTabIndex == 2) {
                                // Lifetime Earnings Banner for Completed Tab
                                val totalEarnings = completedOrders.sumOf { it.earnings ?: 0.0 }
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
                                            modifier = Modifier.size(60.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text("Completed Lifetime Earnings (75%)", style = MaterialTheme.typography.labelSmall)
                                            Text("₦${"%,.2f".format(totalEarnings)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentList) { order ->
                                    FulfillerOrderCard(order, onNavigateToActiveOrder)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FulfillerOrderCard(order: FulfillerOrderResponse, onResume: (String) -> Unit) {
    val statusUpper = order.status?.uppercase() ?: ""
    val isQueued = statusUpper == "QUEUED"
    val canResume = statusUpper.isNotBlank() && statusUpper !in listOf("DELIVERED", "CANCELLED", "RELEASED", "REFUNDED", "RECIPIENT_ABSENT")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Order #${order.id ?: 0}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Text("Agent Fare Share", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("₦${"%.2f".format(order.earnings ?: 0.0)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF388E3C))
                }
                
                if (canResume) {
                    Button(
                        onClick = { onResume(order.id.toString()) },
                        colors = if (isQueued) ButtonDefaults.buttonColors(containerColor = com.ng.pikop.ui.theme.PikopGold, contentColor = Color.Black) else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (isQueued) "START QUEUED" else "RESUME", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = (order.created_at ?: "").take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
