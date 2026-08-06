package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.FulfillerStatusRequest
import com.ng.pikop.core.network.OfferResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FulfillerDashboardScreen(onAcceptOffer: (String) -> Unit) {
    var isOnline by remember { mutableStateOf(false) }
    var offers by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    // Polling for offers when online
    LaunchedEffect(isOnline) {
        while (isOnline) {
            try {
                offers = apiService.getOffers()
            } catch (e: Exception) {
                // Silently handle polling errors in dashboard
            }
            delay(5000) // Poll every 5 seconds
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Fulfiller Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFBE9E7)
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnline) "You are Online" else "You are Offline",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val status = if (checked) "ONLINE" else "OFFLINE"
                                    apiService.updateStatus(FulfillerStatusRequest(status))
                                    isOnline = checked
                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isOnline) {
                Text(
                    text = "Available Offers",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                if (offers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Searching for nearby orders...")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(offers) { offer ->
                            IncomingOfferComponent(
                                offer = offer,
                                onAccept = { onAcceptOffer(offer.id) },
                                onDecline = { /* Handle decline locally or via API */ }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Go online to start receiving delivery offers.")
                }
            }
        }
    }
}
