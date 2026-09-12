package com.ng.pikop.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.OrderDetailsResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderAcknowledgmentScreen(
    orderId: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var order by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isActionLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(orderId) {
        try {
            isLoading = true
            val response = apiService.getOrderDetails(orderId)
            order = response.data ?: response
        } catch (e: Exception) {
            error = ErrorUtils.parseError(e)
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incoming Delivery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        } else if (order != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "A delivery is coming your way!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Order #${order!!.id} from ${order!!.user_name ?: "a Pikop user"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Item Details", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(order!!.item_description ?: "No description provided", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Delivery Address", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(order!!.delivery_address ?: "No address provided", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Please verify your address. We will dispatch an agent as soon as you confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            isActionLoading = true
                            try {
                                apiService.acknowledgeOrder(orderId, mapOf("action" to "confirm"))
                                android.widget.Toast.makeText(context, "Delivery Confirmed!", android.widget.Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, ErrorUtils.parseError(e), android.widget.Toast.LENGTH_LONG).show()
                            } finally { isActionLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActionLoading
                ) {
                    if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Confirm & Verify Address")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isActionLoading = true
                            try {
                                apiService.acknowledgeOrder(orderId, mapOf("action" to "decline"))
                                android.widget.Toast.makeText(context, "Delivery Declined", android.widget.Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, ErrorUtils.parseError(e), android.widget.Toast.LENGTH_LONG).show()
                            } finally { isActionLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    enabled = !isActionLoading
                ) {
                    Text("I wasn't expecting this")
                }
            }
        }
    }
}
