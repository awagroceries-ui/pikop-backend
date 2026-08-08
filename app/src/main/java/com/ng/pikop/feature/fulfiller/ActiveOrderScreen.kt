package com.ng.pikop.feature.fulfiller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.OrderDetailsResponse
import com.ng.pikop.core.network.SocketManager
import com.ng.pikop.core.network.VerifyCodeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ActiveOrderScreen(orderId: String, onOrderCompleted: () -> Unit) {
    var orderDetails by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var orderStatus by remember { mutableStateOf("MATCHED") }
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    
    var pickupCode by remember { mutableStateOf("") }
    var deliveryCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val cameraPositionState = rememberCameraPositionState()

    // Fetch Order Details on Init
    LaunchedEffect(orderId) {
        try {
            val response = apiService.getOrderDetails(orderId)
            orderDetails = response
            orderStatus = response.status
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load order info", Toast.LENGTH_SHORT).show()
        }
    }

    // Socket Connection & Location Tracking
    DisposableEffect(Unit) {
        SocketManager.connect()
        onDispose {
            SocketManager.disconnect()
        }
    }

    LaunchedEffect(orderStatus) {
        if (orderStatus == "MATCHED" || orderStatus == "PICKED_UP") {
            while (true) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                fulfillerLocation = currentLatLng
                                
                                val data = JSONObject().apply {
                                    put("orderId", orderId)
                                    put("lat", location.latitude)
                                    put("lng", location.longitude)
                                }
                                SocketManager.emit("update_location", data)
                            }
                        }
                } catch (e: SecurityException) {}
                delay(10000)
            }
        }
    }

    // Auto-zoom map to fit fulfiller and target
    LaunchedEffect(fulfillerLocation, orderStatus, orderDetails) {
        if (fulfillerLocation != null && orderDetails != null) {
            val target = if (orderStatus == "MATCHED") {
                LatLng(orderDetails!!.pickup_lat, orderDetails!!.pickup_lng) 
            } else {
                LatLng(orderDetails!!.delivery_lat, orderDetails!!.delivery_lng)
            }
            
            val bounds = LatLngBounds.builder()
                .include(fulfillerLocation!!)
                .include(target)
                .build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Map Section (Top 40%)
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    fulfillerLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Your Location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                    
                    if (orderDetails != null) {
                        val targetLatLng = if (orderStatus == "MATCHED") {
                            LatLng(orderDetails!!.pickup_lat, orderDetails!!.pickup_lng)
                        } else {
                            LatLng(orderDetails!!.delivery_lat, orderDetails!!.delivery_lng)
                        }
                        
                        Marker(
                            state = MarkerState(position = targetLatLng),
                            title = if (orderStatus == "MATCHED") "Pickup" else "Delivery",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (orderStatus == "MATCHED") BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }
            }

            // Details Section (Bottom 60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Active Delivery", style = MaterialTheme.typography.headlineMedium)
                Text(text = "Order ID: #$orderId", style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(24.dp))

                if (orderStatus == "MATCHED") {
                    PhaseCard(
                        title = "Phase 1: Pickup",
                        address = orderDetails?.pickup_address ?: "Loading...",
                        buttonText = "Navigate to Pickup",
                        onNavigate = { navigateToAddress(context, orderDetails?.pickup_address ?: "") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pickupCode,
                        onValueChange = { pickupCode = it },
                        label = { Text("Enter 4-digit Pickup Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    apiService.verifyPickup(orderId, VerifyCodeRequest(pickupCode))
                                    orderStatus = "PICKED_UP"
                                    Toast.makeText(context, "Pickup Verified!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid Pickup Code", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && pickupCode.length == 4
                    ) {
                        Text("Verify Pickup")
                    }
                }

                if (orderStatus == "PICKED_UP") {
                    PhaseCard(
                        title = "Phase 2: Delivery",
                        address = orderDetails?.delivery_address ?: "Loading...",
                        recipientPhone = "+234 812 345 6789",
                        buttonText = "Navigate to Delivery",
                        onNavigate = { navigateToAddress(context, orderDetails?.delivery_address ?: "") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = deliveryCode,
                        onValueChange = { deliveryCode = it },
                        label = { Text("Enter 4-digit Delivery Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    apiService.verifyDelivery(orderId, VerifyCodeRequest(deliveryCode))
                                    orderStatus = "DELIVERED"
                                    showRatingDialog = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Invalid Delivery Code", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && deliveryCode.length == 4
                    ) {
                        Text("Complete Delivery")
                    }
                }
            }
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            onDismiss = { 
                showRatingDialog = false
                onOrderCompleted()
            },
            onSubmit = { rating ->
                coroutineScope.launch {
                    try {
                        apiService.rateCustomer(orderId, com.ng.pikop.core.network.RatingRequest(rating))
                    } catch (e: Exception) {}
                    showRatingDialog = false
                    onOrderCompleted()
                }
            }
        )
    }
}

@Composable
fun PhaseCard(
    title: String,
    address: String,
    recipientPhone: String? = null,
    buttonText: String,
    onNavigate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = address, style = MaterialTheme.typography.bodyLarge)
            
            if (recipientPhone != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Recipient: $recipientPhone", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                Text(buttonText)
            }
        }
    }
}

fun navigateToAddress(context: Context, address: String) {
    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
    }
}
