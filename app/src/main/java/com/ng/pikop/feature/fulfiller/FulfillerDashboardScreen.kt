package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.*
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
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
    onCelebration: () -> Unit = {},
    onLogout: () -> Unit
) {
    var isOnline by remember { mutableStateOf(false) }
    var offers by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    var isStatusLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<FulfillerOrderResponse>>(emptyList()) }
    var walletBalance by remember { mutableStateOf(0.0) }
    var profileData by remember { mutableStateOf<com.ng.pikop.core.network.FulfillerProfileResponse?>(null) }
    var agentLocation by remember { mutableStateOf<LatLng?>(null) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val userId by tokenManager.userId.collectAsState(initial = null)

    val fetchDashboardData = {
        coroutineScope.launch {
            try {
                isLoading = true
                val profile = apiService.getFulfillerProfile()
                val prevStreak = profileData?.current_streak_days ?: 0
                val prevCompleted = profileData?.stats?.total_completed ?: 0
                
                profileData = profile.data ?: profile
                isOnline = profileData?.online_status == "ONLINE"

                val newStreak = profileData?.current_streak_days ?: 0
                val newCompleted = profileData?.stats?.total_completed ?: 0
                
                if ((newStreak == 7 && prevStreak < 7) || (newStreak == 30 && prevStreak < 30)) {
                    onCelebration()
                } else if (newCompleted == 1 && prevCompleted == 0) {
                    onCelebration()
                } else if (profileData?.kyc_status == "VERIFIED" && (kycStatus == "PENDING" || kycStatus == "NOT_STARTED")) {
                    onCelebration()
                }
                
                val wallet = apiService.getWalletInfo()
                walletBalance = wallet.balance ?: 0.0
                history = apiService.getFulfillerOrders()
                offers = apiService.getOffers()
            } catch (e: Exception) {
                android.util.Log.e("DashboardRefresh", "Fetch failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Socket Connection & Real-Time Event Listener
    DisposableEffect(userId) {
        val userIdStr = userId?.toString() ?: ""
        if (userIdStr.isNotBlank()) {
            SocketManager.connect(userIdStr)
            SocketManager.on("new_mission_offer") {
                android.util.Log.d("DashboardSocket", "Inbound mission offer via socket. Refreshing...")
                fetchDashboardData()
            }
            SocketManager.on("order_status_updated") {
                android.util.Log.d("DashboardSocket", "Order status update via socket. Refreshing...")
                fetchDashboardData()
            }
            SocketManager.on("status_updated") {
                fetchDashboardData()
            }
        }
        onDispose {}
    }

    // Location Resolution Helper
    val resolveAgentLocation: suspend () -> Unit = @android.annotation.SuppressLint("MissingPermission") {
        try {
            var loc = try { fusedLocationClient.lastLocation.await() } catch (_: Exception) { null }
            if (loc == null) {
                loc = try { fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await() } catch (_: Exception) { null }
            }
            if (loc != null) {
                val latLng = LatLng(loc.latitude, loc.longitude)
                agentLocation = latLng
                
                val state = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()?.adminArea
                    } catch (e: Exception) { null }
                }
                
                apiService.updateStatus(FulfillerStatusRequest(
                    online_status = if (isOnline) "ONLINE" else "OFFLINE",
                    lat = loc.latitude,
                    lng = loc.longitude,
                    current_state = state
                ))
            }
        } catch (_: Exception) {}
    }

    // Initial Fetch on Launch
    LaunchedEffect(Unit) {
        fetchDashboardData()
        resolveAgentLocation()
    }

    // Polling & Background Sync (Runs every 5 seconds for real-time mission updates)
    LaunchedEffect(isOnline) {
        while (true) {
            try {
                if (isOnline) {
                    offers = apiService.getOffers()
                    resolveAgentLocation()
                }
                history = apiService.getFulfillerOrders()
                val wallet = apiService.getWalletInfo()
                walletBalance = wallet.balance ?: 0.0
            } catch (e: Exception) {
                android.util.Log.e("DashboardPoll", "Error: ${e.message}")
            }
            delay(5000) // 5s interval for instant mission updates
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
                    IconButton(onClick = { fetchDashboardData() }) {
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
            var showHotspots by remember { mutableStateOf(false) }
            var hotspots by remember { mutableStateOf<List<LatLng>>(emptyList()) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(agentLocation ?: LatLng(6.5244, 3.3792), 14f)
            }

            // Animate map camera whenever agent's location is resolved
            LaunchedEffect(agentLocation) {
                agentLocation?.let { pos ->
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                }
            }

            LaunchedEffect(showHotspots) {
                if (showHotspots) {
                    try {
                        val res = apiService.getDemandHeatmap()
                        @Suppress("UNCHECKED_CAST")
                        val data = res["data"] as? List<Map<String, Any>>
                        val parsedHotspots = data?.map { LatLng((it["lat"] as? Number)?.toDouble() ?: 0.0, (it["lng"] as? Number)?.toDouble() ?: 0.0) } ?: emptyList()
                        hotspots = parsedHotspots
                    } catch (_: Exception) {}
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // 1. Map Section
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = false)
                        ) {
                            agentLocation?.let { pos ->
                                Marker(
                                    state = MarkerState(position = pos),
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                                    title = "Your Location"
                                )
                            }
                            if (showHotspots) {
                                hotspots.forEach { spot ->
                                    Marker(
                                        state = MarkerState(position = spot),
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                        alpha = 0.6f,
                                        title = "High Demand Zone"
                                    )
                                }
                            }
                        }

                        FilterChip(
                            selected = showHotspots,
                            onClick = { showHotspots = !showHotspots },
                            label = { Text("Demand Hotspots", fontSize = 10.sp) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            leadingIcon = { Icon(Icons.Default.LocalFireDepartment, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                // Active Mission Resume Banner
                val activeMission = history.firstOrNull { 
                    val s = it.status?.uppercase() ?: ""
                    s.isNotBlank() && s != "QUEUED" && s !in listOf("DELIVERED", "CANCELLED", "RELEASED", "REFUNDED", "RECIPIENT_ABSENT") 
                }

                val queuedMission = history.firstOrNull { 
                    (it.status?.uppercase() ?: "") == "QUEUED"
                }

                if (activeMission != null) {
                    item {
                        Card(
                            onClick = { onAcceptOffer(activeMission.id.toString()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DirectionsBike, null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ACTIVE MISSION IN PROGRESS 🚀", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                    Text("Order #${activeMission.id} • ${activeMission.status?.uppercase()}", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onAcceptOffer(activeMission.id.toString()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.ng.pikop.ui.theme.PikopGold, contentColor = Color.Black)
                                ) {
                                    Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (queuedMission != null) {
                    item {
                        Card(
                            onClick = { onAcceptOffer(queuedMission.id.toString()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HourglassTop, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("QUEUED MISSION WAITING ⏳", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp)
                                    Text("Order #${queuedMission.id} • Next in Queue", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { onAcceptOffer(queuedMission.id.toString()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.ng.pikop.ui.theme.PikopGold, contentColor = Color.Black)
                                ) {
                                    Text("START", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // 2. Dash Content
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Peak Hour Bonus Banner
                        val peakActive = profileData?.stats?.peak_active ?: false
                        val peakBonus = profileData?.stats?.peak_bonus ?: 0.0
                        if (peakActive && peakBonus > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onTertiary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Peak Bonus Active! 🔥", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary)
                                        Text("Earn an extra ₦${peakBonus.toInt()} on every delivery.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.9f))
                                    }
                                }
                            }
                        }

                        // Streak Tracker
                        val streak = profileData?.current_streak_days ?: 0
                        if (streak > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("$streak Day Streak", fontWeight = FontWeight.Bold)
                                        val nextMilestone = if (streak < 7) 7 else 30
                                        Text("Keep it up! Next bonus at $nextMilestone days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }

                        // KYC Card
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
                                            color = if (isPending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = if (isPending) "We are reviewing your details. This usually takes 24 hours." else "Complete verification to start earning.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isPending) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Wallet Card
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
                                    TextButton(onClick = onGoToInsights) { Text("Insights", style = MaterialTheme.typography.labelSmall) }
                                    TextButton(onClick = onGoToWallet) { Text("Wallet", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                        }
                        
                        // Status Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
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
                                            isStatusLoading = true
                                            try {
                                                var state: String? = null
                                                if (checked) {
                                                    try {
                                                        val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                                                        state = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            try { Geocoder(context, Locale.getDefault()).getFromLocation(loc?.latitude ?: 0.0, loc?.longitude ?: 0.0, 1)?.firstOrNull()?.adminArea } catch (e: Exception) { null }
                                                        }
                                                    } catch (e: SecurityException) {}
                                                }
                                                apiService.updateStatus(FulfillerStatusRequest(online_status = targetStatus, current_state = state))
                                                isOnline = checked
                                            } catch (e: Exception) {
                                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_LONG).show()
                                            } finally { isStatusLoading = false }
                                        }
                                    },
                                    enabled = !isStatusLoading && kycStatus == "VERIFIED"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Available Offers", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // 3. Offers List
                if (isOnline) {
                    if (offers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Searching for nearby orders...", color = Color.Gray)
                            }
                        }
                    } else {
                        items(offers) { offer ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                IncomingOfferComponent(
                                    offer = offer,
                                    onAccept = {
                                        coroutineScope.launch {
                                            try {
                                                isLoading = true
                                                val response = apiService.acceptOrder(offer.id ?: "", emptyMap())
                                                val claimedStatus = response.status ?: response.data?.status
                                                val isSuccess = claimedStatus == "MATCHED" || claimedStatus == "QUEUED" || 
                                                                response.message?.contains("Accepted", ignoreCase = true) == true || 
                                                                response.message?.contains("Claimed", ignoreCase = true) == true
                                                
                                                if (isSuccess) {
                                                    Toast.makeText(context, "Mission Accepted!", Toast.LENGTH_SHORT).show()
                                                    onAcceptOffer(offer.id ?: "")
                                                } else {
                                                    fetchDashboardData()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_LONG).show()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    onDecline = {
                                        fetchDashboardData()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
