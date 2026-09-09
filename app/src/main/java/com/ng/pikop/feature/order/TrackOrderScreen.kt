package com.ng.pikop.feature.order

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.ng.pikop.feature.fulfiller.FulfillerProfileDialog
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
    val userId by tokenManager.userId.collectAsState(initial = null)
    
    // Animation State
    val interpolatedLat = remember { Animatable(0f) }
    val interpolatedLng = remember { Animatable(0f) }
    val animatedFulfillerLoc by remember {
        derivedStateOf {
            if (interpolatedLat.value != 0f) LatLng(interpolatedLat.value.toDouble(), interpolatedLng.value.toDouble())
            else fulfillerLocation
        }
    }
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

    DisposableEffect(orderId, userId) {
        if (userId != null) {
            SocketManager.connect(userId)
            SocketManager.emit("join_order", orderId)
        }
        
        val handleLocationUpdate: (JSONObject) -> Unit = { data ->
            val lat = data.optDouble("lat", 0.0)
            val lng = data.optDouble("lng", 0.0)
            if (lat != 0.0 && lng != 0.0) {
                val newLoc = LatLng(lat, lng)
                
                coroutineScope.launch {
                    if (interpolatedLat.value == 0f) {
                        interpolatedLat.snapTo(lat.toFloat())
                        interpolatedLng.snapTo(lng.toFloat())
                    } else {
                        launch { interpolatedLat.animateTo(lat.toFloat(), tween(2000)) }
                        launch { interpolatedLng.animateTo(lng.toFloat(), tween(2000)) }
                    }
                }

                fulfillerLocation = newLoc
                deliveryLoc?.let { dest ->
                    val distanceKm = calculateDistance(newLoc, dest)
                    etaMinutes = ((distanceKm / 30.0) * 60).roundToInt().coerceAtLeast(1)
                }
            }
        }

        SocketManager.on("location_updated", handleLocationUpdate)
        SocketManager.on("location_changed", handleLocationUpdate)
        SocketManager.on("status_updated") { _ -> fetchHistory() }

        onDispose {
            SocketManager.off("location_updated")
            SocketManager.off("location_changed")
            SocketManager.off("status_updated")
            SocketManager.disconnect()
        }
    }

    LaunchedEffect(pickupLoc, deliveryLoc, fulfillerLocation) {
        val p = pickupLoc
        val d = deliveryLoc
        val f = fulfillerLocation
        val defaultLagos = LatLng(6.5244, 3.3792)

        val validP = if (p != null && p.latitude != 0.0) p else null
        val validD = if (d != null && d.latitude != 0.0) d else null
        val validF = if (f != null && f.latitude != 0.0) f else null

        try {
            val builder = LatLngBounds.builder()
            var count = 0
            validP?.let { builder.include(it); count++ }
            validD?.let { builder.include(it); count++ }
            validF?.let { builder.include(it); count++ }

            if (count >= 2) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
            } else if (count == 1) {
                val singlePoint = validP ?: validD ?: validF ?: defaultLagos
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(singlePoint, 15f))
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLagos, 12f))
            }
        } catch (e: Exception) {
            val fallbackPoint = validP ?: validD ?: validF ?: defaultLagos
            cameraPositionState.position = CameraPosition.fromLatLngZoom(fallbackPoint, 14f)
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
                        animatedFulfillerLoc?.let { Marker(state = MarkerState(position = it), title = "Agent", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) }
                        
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
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    
    var orderDetails by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showFulfillerProfile by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(orderId, refreshKey) {
        try {
            val res = apiService.getOrderDetails(orderId)
            val data = res.data ?: res
            orderDetails = data
            
            // Auto-show rating if delivered and not yet rated
            if ((data.status == "DELIVERED" || data.status == "RELEASED") && data.customer_rating == null) {
                showRatingDialog = true
            }
        } catch (e: Exception) {}
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status & Secure Pay Banner
            val currentStatus = orderDetails?.status?.uppercase() ?: ""
            val isPendingConfirmation = currentStatus == "DELIVERED_PENDING_CONFIRMATION"

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = if (eta != null) "Arriving in $eta mins" else "Status: ${currentStatus.replace('_', ' ')}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = "Order #$orderId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            if (isPendingConfirmation) {
                Spacer(modifier = Modifier.height(16.dp))
                // ... (existing code for pending confirmation)
            }

            // Pickup & Delivery Codes for Customer
            if (orderDetails?.pickup_code != null || orderDetails?.delivery_code != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Confirmation Codes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Share these with the agent at handoff.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val pCode = orderDetails?.pickup_code
                            if (pCode != null) {
                                CodeBox("Pickup", pCode, Modifier.weight(1f))
                            }
                            val dCode = orderDetails?.delivery_code
                            if (dCode != null) {
                                CodeBox("Delivery", dCode, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if ((currentStatus == "DELIVERED" || currentStatus == "RELEASED") && orderDetails?.customer_rating == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showRatingDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F0A))
                ) {
                    Icon(Icons.Default.Star, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate Delivery Experience")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            profile?.let { 
                Box(modifier = Modifier.clickable { showFulfillerProfile = true }) {
                    FulfillerCard(it)
                }
                Spacer(modifier = Modifier.height(16.dp)) 
            }
            
            if (showFulfillerProfile && profile != null) {
                FulfillerProfileDialog(profile = profile!!, onDismiss = { showFulfillerProfile = false })
            }

            if (showDisputeDialog) {
                SecurePayDisputeDialog(
                    onDismiss = { showDisputeDialog = false },
                    onConfirm = { reason, notes ->
                        coroutineScope.launch {
                            try {
                                apiService.reportProblem(orderId, mapOf("reason" to reason, "notes" to notes))
                                android.widget.Toast.makeText(context, "Issue Reported. Support will investigate.", android.widget.Toast.LENGTH_LONG).show()
                                showDisputeDialog = false
                                onRefresh()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Report failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            if (showRatingDialog) {
                FulfillerRatingDialog(
                    onDismiss = { showRatingDialog = false },
                    onConfirm = { rating, comment ->
                        coroutineScope.launch {
                            try {
                                apiService.rateFulfiller(orderId, RatingRequest(rating, comment))
                                android.widget.Toast.makeText(context, "Thank you for your rating!", android.widget.Toast.LENGTH_SHORT).show()
                                showRatingDialog = false
                                onRefresh()
                            } catch (e: Exception) {
                                val errorMsg = com.ng.pikop.core.network.ErrorUtils.parseError(e)
                                android.widget.Toast.makeText(context, "Rating failed: $errorMsg", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
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
fun FulfillerRatingDialog(onDismiss: () -> Unit, onConfirm: (Int, String?) -> Unit) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your Experience") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How was your delivery agent?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { i ->
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= rating) Color(0xFFFF9F0A) else Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Add a comment (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(rating, comment.ifBlank { null }) }) {
                Text("Submit Rating")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}

@Composable
fun SecurePayDisputeDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("Item Damaged") }
    var notes by remember { mutableStateOf("") }
    val reasons = listOf("Item Damaged", "Wrong Item", "Not as Described", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report a Problem") },
        text = {
            Column {
                Text("Select reason:", style = MaterialTheme.typography.labelSmall)
                reasons.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(reason, notes) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

@Composable
fun CodeBox(label: String, code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .clickable {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Pikop Code", code)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "$label code copied!", android.widget.Toast.LENGTH_SHORT).show()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
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

fun formatCountdown(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "--:--"
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val target = parser.parse(isoTimestamp!!)?.time ?: return "--:--"
        val diff = target - System.currentTimeMillis()
        if (diff <= 0) return "Releasing now..."
        
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff / (1000 * 60)) % 60
        "${hours}h ${minutes}m"
    } catch (e: Exception) { "--:--" }
}
