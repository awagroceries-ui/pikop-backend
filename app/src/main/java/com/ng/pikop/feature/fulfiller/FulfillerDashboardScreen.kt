package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.ng.pikop.core.network.FulfillerStatusRequest
import com.ng.pikop.core.network.OfferResponse
import com.ng.pikop.feature.auth.NavigationDrawerContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillerDashboardScreen(
    userEmail: String,
    onAcceptOffer: (String) -> Unit, 
    onGoToWallet: () -> Unit,
    onGoToKyc: () -> Unit,
    onGoToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    var isOnline by remember { mutableStateOf(false) }
    var offers by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var kycStatus by remember { mutableStateOf("PENDING") }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    // Initial Fetch
    LaunchedEffect(Unit) {
        try {
            val profile = apiService.getFulfillerProfile()
            kycStatus = profile.kyc_status ?: "PENDING"
            isOnline = profile.online_status == "ONLINE"
            history = apiService.getFulfillerOrders()
        } catch (e: Exception) {}
    }

    // Polling
    LaunchedEffect(isOnline) {
        while (isOnline) {
            try {
                offers = apiService.getOffers()
            } catch (e: Exception) {}
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pikop Fulfiller") },
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding).fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // KYC Warning
                if (kycStatus != "VERIFIED") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        onClick = onGoToKyc
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.pikop_badge),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Account Not Verified", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                Text("Complete verification to start earning.", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = onGoToKyc) {
                                Text("Verify Now")
                            }
                        }
                    }
                }

                // Earnings Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    onClick = onGoToWallet,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Earnings", style = MaterialTheme.typography.labelSmall)
                            val total = history.sumOf { it.earnings ?: 0.0 }
                            Text("₦${"%,.2f".format(total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onGoToWallet) {
                            Text("Wallet", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Online/Offline Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFBE9E7))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isOnline) "You are Online" else "You are Offline", style = MaterialTheme.typography.titleMedium, color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828))
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        apiService.updateStatus(FulfillerStatusRequest(if (checked) "ONLINE" else "OFFLINE"))
                                        isOnline = checked
                                    } catch (e: Exception) {
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && kycStatus == "VERIFIED"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isOnline) {
                    Text(text = "Available Offers", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (offers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Searching for nearby orders...")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(offers) { offer ->
                                IncomingOfferComponent(
                                    offer = offer,
                                    onAccept = { onAcceptOffer(offer.id ?: "") },
                                    onDecline = { offers = offers.filter { it.id != offer.id } }
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = if (kycStatus == "VERIFIED") "Go online to start receiving offers." else "Verify your account to start receiving offers.")
                    }
                }
            }
        }
    }
}
