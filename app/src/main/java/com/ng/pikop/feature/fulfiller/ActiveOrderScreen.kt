package com.ng.pikop.feature.fulfiller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.IncidentRequest
import com.ng.pikop.core.network.OfferResponse
import com.ng.pikop.core.network.OrderDetailsResponse
import com.ng.pikop.core.network.SocketManager
import com.ng.pikop.core.network.VerifyCodeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@Composable
fun ActiveOrderScreen(orderId: String, onOrderCompleted: () -> Unit) {
    var orderDetails by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var orderStatus by remember { mutableStateOf("MATCHED") }
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    
    var queueCandidates by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    var queuedOrder by remember { mutableStateOf<OfferResponse?>(null) }
    
    var pickupCode by remember { mutableStateOf("") }
    var deliveryCode by remember { mutableStateOf("") }
    var deliveryPhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showIncidentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val cameraPositionState = rememberCameraPositionState()

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        deliveryPhotoUri = uri
    }

    // Fetch Order Details on Init
    LaunchedEffect(orderId) {
        try {
            val response = apiService.getOrderDetails(orderId)
            orderDetails = response
            orderStatus = response.status
            
            // Fetch queue candidates if eligible
            if (response.status == "PICKED_UP") {
                queueCandidates = apiService.getQueueCandidates()
            }
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

    // Auto-zoom map
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
                
                // Incident Button
                IconButton(
                    onClick = { showIncidentDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = "Report Incident", tint = MaterialTheme.colorScheme.error)
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
                Text(text = "Active Mission", style = MaterialTheme.typography.headlineMedium)
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
                                    // Fetch queue candidates after pickup success
                                    queueCandidates = apiService.getQueueCandidates()
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

                    // Queuing Section
                    if (queuedOrder == null) {
                        if (queueCandidates.isNotEmpty()) {
                            Text(text = "Queue Your Next Mission", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(8.dp))
                            queueCandidates.forEach { candidate ->
                                IncomingOfferComponent(
                                    offer = candidate,
                                    onAccept = {
                                        coroutineScope.launch {
                                            try {
                                                apiService.claimQueueOrder(candidate.id)
                                                queuedOrder = candidate
                                                Toast.makeText(context, "Mission Queued!", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to queue mission.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onDecline = { queueCandidates = queueCandidates.filter { it.id != candidate.id } }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = "UP NEXT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(text = "Mission Queued: ${queuedOrder!!.pickup_address}")
                                    Text(text = "Starts automatically after current delivery.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Mandatory Delivery Photo
                    Card(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (deliveryPhotoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (deliveryPhotoUri != null) "Photo Attached" else "Take Proof of Delivery")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    // 1. Upload delivery photo
                                    val file = getFileFromUri(context, deliveryPhotoUri!!)
                                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
                                    val uploadRes = apiService.uploadOrderPhoto(body)
                                    val photoUrl = uploadRes["url"] ?: ""

                                    // 2. Verify delivery
                                    apiService.verifyDelivery(orderId, VerifyCodeRequest(deliveryCode, photoUrl))
                                    orderStatus = "DELIVERED"
                                    showRatingDialog = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Delivery Verification Failed", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && deliveryCode.length == 4 && deliveryPhotoUri != null
                    ) {
                        Text("Complete Mission")
                    }
                }
            }
        }
    }

    if (showIncidentDialog) {
        IncidentReportDialog(
            onDismiss = { showIncidentDialog = false },
            onReport = { category, resolution, notes ->
                coroutineScope.launch {
                    try {
                        apiService.fileIncident(orderId, IncidentRequest(category, notes, resolution))
                        Toast.makeText(context, "Incident reported. Check status.", Toast.LENGTH_LONG).show()
                        onOrderCompleted() // Navigate away
                    } catch (e: Exception) {}
                    showIncidentDialog = false
                }
            }
        )
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
fun IncidentReportDialog(onDismiss: () -> Unit, onReport: (String, String, String) -> Unit) {
    var category by remember { mutableStateOf("breakdown") }
    var resolution by remember { mutableStateOf("handoff") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Incident") },
        text = {
            Column {
                Text("What happened?", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == "breakdown", onClick = { category = "breakdown" }, label = { Text("Breakdown") })
                    FilterChip(selected = category == "security_risk", onClick = { category = "security_risk" }, label = { Text("Security") })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Requested Resolution", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = resolution == "handoff", onClick = { resolution = "handoff" }, label = { Text("Handoff") })
                    FilterChip(selected = resolution == "cancel_with_waiver_request", onClick = { resolution = "cancel_with_waiver_request" }, label = { Text("Abort") })
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onReport(category, resolution, notes) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("File Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
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
