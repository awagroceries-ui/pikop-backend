package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.withContext
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillerDashboardScreen(
    userEmail: String,
    kycStatus: String,
    onAcceptOffer: (String) -> Unit, 
    onGoToWallet: () -> Unit,
    onGoToKyc: () -> Unit,
    onGoToInsights: () -> Unit,
    onGoToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    var isOnline by remember { mutableStateOf(false) }
    var offers by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var walletBalance by remember { mutableStateOf(0.0) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Active Mission Listener (Socket)
    LaunchedEffect(isOnline) {
        if (isOnline) {
            com.ng.pikop.core.network.SocketManager.on("new_mission_offer") {
                android.util.Log.d("DashboardSocket", "New mission offer received via socket. Refreshing...")
                coroutineScope.launch {
                    try {
                        offers = apiService.getOffers()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Initial Fetch: Sync Wallet & History
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val wallet = apiService.getWalletInfo()
            walletBalance = wallet.balance ?: 0.0
            history = apiService.getFulfillerOrders()
        } catch (_: Exception) {
        } finally {
            isLoading = false
        }
    }

    // Polling & Background Pings
    LaunchedEffect(isOnline) {
        while (isOnline) {
            try {
                // 1. Fetch Offers
                offers = apiService.getOffers()
                
                // 2. Refresh Wallet & History (Real-time stats)
                val wallet = apiService.getWalletInfo()
                walletBalance = wallet.balance ?: 0.0
                history = apiService.getFulfillerOrders()

                // 3. Location & State PING (Idle Fleet Management)
                try {
                    val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                    if (location != null) {
                        val state = withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                @Suppress("DEPRECATION")
                                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.adminArea
                            } catch (e: Exception) { null }
                        }
                        
                        apiService.updateStatus(FulfillerStatusRequest(
                            online_status = "ONLINE",
                            lat = location.latitude,
                            lng = location.longitude,
                            current_state = state
                        ))
                        android.util.Log.d("FleetPing", "Background PING sent: ${location.latitude}, ${location.longitude} ($state)")
                    }
                } catch (e: SecurityException) {}

            } catch (e: Exception) {
                android.util.Log.e("DashboardPoll", "Error: ${e.message}")
            }
            delay(60000) // 60s interval for background sync
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pikop Fulfiller")
                        if (kycStatus == "VERIFIED") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Image(
                                painter = painterResource(id = R.drawable.pikop_badge),
                                contentDescription = "Verified",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
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
                // Resume Active Mission Banner
                val activeMissions = history.filter { 
                    it.status != "DELIVERED" && it.status != "CANCELLED" && it.status != "RECIPIENT_ABSENT" && it.status != "RELEASED" && it.status != "REFUNDED"
                }
                if (activeMissions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = { onAcceptOffer(activeMissions.first().id.toString()) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resume Active Mission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("You have a mission in progress. Tap to return.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                // KYC Status Card
                if (kycStatus != "VERIFIED") {
                    val isPending = kycStatus == "PENDING_REVIEW"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPending) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.errorContainer
                        ),
                        onClick = onGoToKyc
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPending) Icons.Default.Pending else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isPending) Color(0xFFFF9800) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPending) "Verification Under Review" else "Account Not Verified",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isPending) Color(0xFFE65100) else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (isPending) "We are reviewing your details. This usually takes 24 hours." else "Complete verification to start earning.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isPending) Color(0xFFE65100).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                            if (!isPending && kycStatus != "REJECTED") {
                                TextButton(onClick = onGoToKyc) {
                                    Text("Verify Now")
                                }
                            } else if (kycStatus == "REJECTED") {
                                TextButton(onClick = onGoToKyc) {
                                    Text("Fix Issues")
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    onClick = onGoToWallet,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Withdrawable Balance", style = MaterialTheme.typography.labelSmall)
                            Text("₦${"%,.2f".format(walletBalance)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            TextButton(onClick = onGoToInsights) {
                                Text("Insights", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = onGoToWallet) {
                                Text("Wallet", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                
                // Online/Offline Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOnline) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isOnline) "You are Online" else "You are Offline", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Switch(
                            checked = isOnline,
                            onCheckedChange = { checked ->
                                val targetStatus = if (checked) "ONLINE" else "OFFLINE"
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        android.util.Log.d("FleetStatus", "Updating status to: $targetStatus")
                                        
                                        // Resolve current state before going online
                                        var state: String? = null
                                        if (checked) {
                                            try {
                                                val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                                                state = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                    try {
                                                        val geocoder = Geocoder(context, Locale.getDefault())
                                                        @Suppress("DEPRECATION")
                                                        geocoder.getFromLocation(loc?.latitude ?: 0.0, loc?.longitude ?: 0.0, 1)?.firstOrNull()?.adminArea
                                                    } catch (e: Exception) { null }
                                                }
                                            } catch (e: SecurityException) {}
                                        }

                                        apiService.updateStatus(FulfillerStatusRequest(
                                            online_status = targetStatus,
                                            current_state = state
                                        ))
                                        
                                        isOnline = checked
                                        android.widget.Toast.makeText(context, "Status updated: $targetStatus", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        val errorMsg = com.ng.pikop.core.network.ErrorUtils.parseError(e)
                                        android.util.Log.e("FleetStatus", "Update failed: $errorMsg", e)
                                        android.widget.Toast.makeText(context, "Failed to update status: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && kycStatus == "VERIFIED",
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
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
