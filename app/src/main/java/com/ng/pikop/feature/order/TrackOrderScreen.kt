package com.ng.pikop.feature.order

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.SocketManager
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
fun TrackOrderScreen(orderId: String, pickup: LatLng, delivery: LatLng) {
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    var etaMinutes by remember { mutableStateOf<Int?>(null) }
    var history by remember { mutableStateOf<List<OrderStatusStep>>(emptyList()) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickup, 14f)
    }

    val fetchHistory: () -> Unit = {
        coroutineScope.launch {
            try {
                val details = apiService.getOrderDetails(orderId)
                history = details.history.map { item ->
                    OrderStatusStep(
                        status = item.status,
                        description = item.description,
                        time = formatTime(item.time),
                        isCompleted = true
                    )
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(orderId) {
        fetchHistory()
    }

    DisposableEffect(orderId) {
        SocketManager.connect()
        SocketManager.emit("join_order", JSONObject().put("orderId", orderId))
        
        SocketManager.on("location_changed") { data ->
            val lat = data.getDouble("lat")
            val lng = data.getDouble("lng")
            val newLoc = LatLng(lat, lng)
            fulfillerLocation = newLoc
            
            val distanceKm = calculateDistance(newLoc, delivery)
            etaMinutes = ((distanceKm / 30.0) * 60).roundToInt().coerceAtLeast(1)
        }

        SocketManager.on("status_updated") { _ ->
            fetchHistory()
        }

        onDispose {
            SocketManager.off("location_changed")
            SocketManager.off("status_updated")
            SocketManager.disconnect()
        }
    }

    LaunchedEffect(fulfillerLocation) {
        fulfillerLocation?.let { fulfiller ->
            val bounds = LatLngBounds.builder()
                .include(pickup)
                .include(delivery)
                .include(fulfiller)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 140.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            TrackingBottomSheetContent(orderId, etaMinutes, history, tokenManager, fetchHistory)
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false)
            ) {
                Marker(
                    state = MarkerState(position = pickup),
                    title = "Pickup Point",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                Marker(
                    state = MarkerState(position = delivery),
                    title = "Delivery Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                fulfillerLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Driver Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                Polyline(
                    points = listOf(pickup, delivery),
                    color = Color.Gray,
                    width = 5f,
                    pattern = listOf(Dash(20f), Gap(10f))
                )
            }
        }
    }
}

@Composable
fun TrackingBottomSheetContent(
    orderId: String,
    eta: Int?,
    history: List<OrderStatusStep>,
    tokenManager: TokenManager,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color.LightGray, CircleShape)
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (eta != null) "Arriving in $eta mins" else "Finding best route...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Order #$orderId",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp)

        // Cancel Order Button
        val canCancel = history.none { it.status == "PICKED_UP" || it.status == "DELIVERED" || it.status == "CANCELLED" }
        val isMatched = history.any { it.status == "MATCHED" }
        
        if (canCancel) {
            val scope = rememberCoroutineScope()
            val apiService = remember { ApiService.create(tokenManager) }
            var showCancelConfirm by remember { mutableStateOf(false) }

            OutlinedButton(
                onClick = { showCancelConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancel Delivery")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (showCancelConfirm) {
                AlertDialog(
                    onDismissRequest = { showCancelConfirm = false },
                    title = { Text("Abort Mission?") },
                    text = {
                        Text(
                            if (isMatched) "A fulfiller is already on the way. Aborting now will incur a 25% cancellation fee."
                            else "Are you sure you want to cancel this delivery request?"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        apiService.cancelOrder(orderId, mapOf("reason" to "User requested cancellation"))
                                        onRefresh()
                                    } catch (e: Exception) {}
                                }
                                showCancelConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Confirm Abort")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelConfirm = false }) { Text("Keep Order") }
                    }
                )
            }
        }

        Text(text = "Delivery Progress", style = MaterialTheme.typography.titleMedium)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Text("No progress updates yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } else {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(history) { step ->
                    TimelineItem(step)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TimelineItem(step: OrderStatusStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (step.isCompleted) MaterialTheme.colorScheme.primary else Color.LightGray,
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(Color.LightGray)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = step.status,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = if (step.isCompleted) Color.Unspecified else Color.Gray
                )
                Text(text = step.time, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(text = step.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

fun calculateDistance(start: LatLng, end: LatLng): Double {
    val r = 6371
    val dLat = Math.toRadians(end.latitude - start.latitude)
    val dLon = Math.toRadians(end.longitude - start.longitude)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun formatTime(isoTimestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoTimestamp)
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        formatter.format(date!!)
    } catch (e: Exception) {
        isoTimestamp
    }
}
