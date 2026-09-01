package com.ng.pikop.feature.order

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class OrderStatusStep(
    val status: String,
    val description: String,
    val time: String,
    val isCompleted: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(
    orderId: String, 
    pickup: LatLng? = null, 
    delivery: LatLng? = null
) {
    var pickupLoc by remember { mutableStateOf(pickup) }
    var deliveryLoc by remember { mutableStateOf(delivery) }
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    var etaMinutes by remember { mutableStateOf<Int?>(null) }
    var history by remember { mutableStateOf<List<OrderStatusStep>>(emptyList()) }
    var fulfillerProfile by remember { mutableStateOf<FulfillerPublicProfile?>(null) }
    var trackingUrl by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLoc ?: LatLng(6.5244, 3.3792), 14f)
    }

    val fetchHistory: () -> Unit = {
        coroutineScope.launch {
            try {
                isLoading = true
                error = null
                android.util.Log.d("TrackOrder", "Fetching details for mission: $orderId")
                val response = apiService.getOrderDetails(orderId)
                
                // DUAL-COMPATIBILITY: Support both {id: ...} and {data: {id: ...}}
                val details = response.data ?: response
                
                android.util.Log.d("TrackOrder", "Effective Data: $details")
                
                fulfillerProfile = details.fulfiller_profile
                trackingUrl = details.tracking_url
                
                if (details.pickup_lat != null && details.pickup_lng != null) {
                    pickupLoc = LatLng(details.pickup_lat, details.pickup_lng)
                }
                if (details.delivery_lat != null && details.delivery_lng != null) {
                    deliveryLoc = LatLng(details.delivery_lat, details.delivery_lng)
                }
                
                if (details.fulfiller_lat != null && details.fulfiller_lng != null) {
                    fulfillerLocation = LatLng(details.fulfiller_lat, details.fulfiller_lng)
                }

                history = details.history?.map { item ->
                    OrderStatusStep(
                        status = item.status ?: "UNKNOWN",
                        description = item.description ?: "",
                        time = formatTime(item.time ?: ""),
                        isCompleted = true
                    )
                } ?: emptyList()

                if (pickupLoc == null || deliveryLoc == null) {
                    android.util.Log.e("TrackOrder", "Coordinates missing in object: $details")
                    error = "Mission coordinates are missing from server response."
                }
            } catch (e: Exception) {
                android.util.Log.e("TrackOrder", "Fetch failed", e)
                error = "Failed to load mission details: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(orderId) {
        fetchHistory()
    }

    DisposableEffect(orderId) {
        SocketManager.connect()
        SocketManager.emit("join_order", JSONObject().put("orderId", orderId))
        
        SocketManager.on("location_changed") { data ->
            val lat = data.optDouble("lat", 0.0)
            val lng = data.optDouble("lng", 0.0)
            if (lat != 0.0 && lng != 0.0) {
                val newLoc = LatLng(lat, lng)
                fulfillerLocation = newLoc
                deliveryLoc?.let { dest ->
                    val distanceKm = calculateDistance(newLoc, dest)
                    etaMinutes = ((distanceKm / 30.0) * 60).roundToInt().coerceAtLeast(1)
                }
            }
        }

        SocketManager.on("status_updated") { _ -> fetchHistory() }

        onDispose {
            SocketManager.off("location_changed")
            SocketManager.off("status_updated")
            SocketManager.disconnect()
        }
    }

    LaunchedEffect(pickupLoc, deliveryLoc, fulfillerLocation) {
        val p = pickupLoc
        val d = deliveryLoc
        if (p != null && d != null) {
            val builder = LatLngBounds.builder().include(p).include(d)
            fulfillerLocation?.let { builder.include(it) }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Mission Tracking", color = MaterialTheme.colorScheme.primary) },
                actions = {
                    trackingUrl?.let { url ->
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Track my Pikop delivery live at: $url")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share tracking link"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        sheetPeekHeight = 160.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.primary,
        sheetContent = {
            TrackingBottomSheetContent(orderId, etaMinutes, history, fulfillerProfile, tokenManager, fetchHistory)
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error!!, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = fetchHistory) { Text("Retry") }
                }
            } else {
                Box {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false)
                    ) {
                        pickupLoc?.let { Marker(state = MarkerState(position = it), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) }
                        deliveryLoc?.let { Marker(state = MarkerState(position = it), title = "Delivery", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)) }
                        fulfillerLocation?.let { Marker(state = MarkerState(position = it), title = "Agent", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) }
                        
                        if (pickupLoc != null && deliveryLoc != null) {
                            Polyline(points = listOf(pickupLoc!!, deliveryLoc!!), color = Color.Gray, width = 5f, pattern = listOf(Dash(20f), Gap(10f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingBottomSheetContent(orderId: String, eta: Int?, history: List<OrderStatusStep>, profile: FulfillerPublicProfile?, tokenManager: TokenManager, onRefresh: () -> Unit) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (eta != null) "Arriving in $eta mins" else "Status: ${history.lastOrNull()?.status ?: "Processing"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Order #$orderId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            profile?.let { FulfillerCard(it); Spacer(modifier = Modifier.height(16.dp)) }
            val canCancel = history.none { it.status == "PICKED_UP" || it.status == "DELIVERED" || it.status == "CANCELLED" }
            if (canCancel) {
                val scope = rememberCoroutineScope()
                val apiService = remember { ApiService.create(tokenManager) }
                var showCancelConfirm by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showCancelConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Cancel Delivery") }
                if (showCancelConfirm) {
                    AlertDialog(
                        onDismissRequest = { showCancelConfirm = false }, 
                        title = { Text("Abort Mission?") }, 
                        text = { Text("Are you sure you want to cancel this delivery request?") }, 
                        confirmButton = { 
                            Button(
                                onClick = { 
                                    scope.launch { 
                                        try { 
                                            apiService.cancelOrder(orderId, mapOf("reason" to "User requested cancellation"))
                                            android.widget.Toast.makeText(context, "Mission Aborted", android.widget.Toast.LENGTH_SHORT).show()
                                            onRefresh() 
                                        } catch (e: Exception) {
                                            val errorMsg = com.ng.pikop.core.network.ErrorUtils.parseError(e)
                                            android.util.Log.e("TrackOrder", "Cancel failed: $errorMsg", e)
                                            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                                        } 
                                    }
                                    showCancelConfirm = false 
                                }, 
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { 
                                Text("Confirm Abort") 
                            } 
                        }, 
                        dismissButton = { 
                            TextButton(onClick = { showCancelConfirm = false }) { 
                                Text("Keep Order") 
                            } 
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(text = "Delivery Progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            if (history.isEmpty()) { Text("No updates yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) } 
            else { LazyColumn(modifier = Modifier.height(300.dp)) { items(history) { step -> TimelineItem(step) } } }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun FulfillerCard(profile: FulfillerPublicProfile) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) { Text((profile.full_name ?: "A").take(1), color = Color.Black, fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = profile.full_name ?: "Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (profile.kyc_status == "VERIFIED") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Image(
                            painter = painterResource(id = com.ng.pikop.R.drawable.pikop_badge),
                            contentDescription = "Verified",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tierColor = when(profile.tier) { "gold" -> Color(0xFFFFD700); "silver" -> Color(0xFFC0C0C0); else -> Color(0xFFCD7F32) }
                    Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp), tint = tierColor)
                    Text(text = " ${(profile.tier ?: "bronze").uppercase()} AGENT", style = MaterialTheme.typography.labelSmall, color = tierColor)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                profile.vehicle_registration_number?.let { Text(text = it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9F0A)); Text(text = " ${profile.rating_avg ?: 0.0}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun TimelineItem(step: OrderStatusStep) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(12.dp).background(if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape))
            Box(modifier = Modifier.width(2.dp).height(40.dp).background(Color.Gray))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = step.status.replace('_', ' '), style = MaterialTheme.typography.bodyLarge, fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Normal, color = if (step.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Text(text = step.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            }
            Text(text = step.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        }
    }
}

fun calculateDistance(start: LatLng, end: LatLng): Double {
    val r = 6371; val dLat = Math.toRadians(end.latitude - start.latitude); val dLon = Math.toRadians(end.longitude - start.longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}

fun formatTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoTimestamp!!)
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(date!!)
    } catch (e: Exception) { "" }
}
