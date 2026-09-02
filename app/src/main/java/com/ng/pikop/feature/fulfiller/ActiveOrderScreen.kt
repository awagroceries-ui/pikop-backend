package com.ng.pikop.feature.fulfiller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import com.ng.pikop.core.ui.SignaturePad
import android.graphics.Bitmap

@Composable
fun ActiveOrderScreen(orderId: String, onOrderCompleted: () -> Unit, onNavigateToChat: (String) -> Unit) {
    var orderDetails by remember { mutableStateOf<OrderDetailsResponse?>(null) }
    var orderStatus by remember { mutableStateOf("MATCHED") }
    var fulfillerLocation by remember { mutableStateOf<LatLng?>(null) }
    var isFetchingDetails by remember { mutableStateOf(true) }
    
    var queueCandidates by remember { mutableStateOf<List<OfferResponse>>(emptyList()) }
    
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
    
    val userId by tokenManager.userId.collectAsState(initial = null)
    val cameraPositionState = rememberCameraPositionState()

    // Secure Photo Storage for POD
    val podFile = remember { File(context.cacheDir, "pod_${orderId}.jpg") }
    val podUri by remember {
        derivedStateOf {
            try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", podFile)
            } catch (e: Exception) {
                null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) deliveryPhotoUri = podUri
    }

    // Fetch Order Details on Init
    LaunchedEffect(orderId) {
        isFetchingDetails = true
        try {
            val response = apiService.getOrderDetails(orderId)
            orderDetails = response
            orderStatus = (response.status ?: "MATCHED").uppercase()
            
            if (orderStatus == "PICKED_UP") {
                queueCandidates = apiService.getQueueCandidates()
            }
        } catch (e: Exception) {
            Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
        } finally {
            isFetchingDetails = false
        }
    }

    // Socket Connection & Location Tracking
    DisposableEffect(userId) {
        SocketManager.connect(userId)
        onDispose {
            SocketManager.disconnect()
        }
    }

    LaunchedEffect(orderStatus) {
        val normStatus = orderStatus.uppercase()
        if (normStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED", "PICKED_UP", "IN_TRANSIT")) {
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
                                SocketManager.emit("update_mission_location", data)
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
            val normStatus = orderStatus.uppercase()
            val target = if (normStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED")) {
                LatLng(orderDetails?.pickup_lat ?: 0.0, orderDetails?.pickup_lng ?: 0.0) 
            } else {
                LatLng(orderDetails?.delivery_lat ?: 0.0, orderDetails?.delivery_lng ?: 0.0)
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
                        val normStatus = orderStatus.uppercase()
                        val targetLatLng = if (normStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED")) {
                            LatLng(orderDetails?.pickup_lat ?: 0.0, orderDetails?.pickup_lng ?: 0.0)
                        } else {
                            LatLng(orderDetails?.delivery_lat ?: 0.0, orderDetails?.delivery_lng ?: 0.0)
                        }
                        
                        Marker(
                            state = MarkerState(position = targetLatLng),
                            title = if (normStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED")) "Pickup" else "Delivery",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (normStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED")) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }
                
                IconButton(
                    onClick = { showIncidentDialog = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).padding(top = 64.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = "Report Incident", tint = MaterialTheme.colorScheme.error)
                }

                // Chat Button
                IconButton(
                    onClick = { onNavigateToChat(orderId) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
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

                Spacer(modifier = Modifier.height(16.dp))

                if (isFetchingDetails) {
                    Spacer(modifier = Modifier.height(32.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Loading mission details...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    val normalizedStatus = orderStatus.uppercase()
                    
                    val isPickupPhase = normalizedStatus in listOf("MATCHED", "SEARCHING", "ASSIGNED", "ACCEPTED", "PENDING", "PAID")
                    val isDeliveryPhase = normalizedStatus in listOf("PICKED_UP", "IN_TRANSIT", "ARRIVED_AT_DELIVERY")
                    val isQueued = normalizedStatus == "QUEUED"

                    if (isPickupPhase) {
                        PhaseCard(
                            title = "Phase 1: Pickup",
                            address = orderDetails?.pickup_address ?: "Address unavailable",
                            recipientName = orderDetails?.recipient_name,
                            recipientPhone = orderDetails?.recipient_phone,
                            buttonText = "Navigate to Pickup",
                            onNavigate = { 
                                navigateToLocation(
                                    context, 
                                    orderDetails?.pickup_address ?: "",
                                    orderDetails?.pickup_lat,
                                    orderDetails?.pickup_lng
                                ) 
                            },
                            onCallRecipient = {
                                orderDetails?.recipient_phone?.let { phone -> callPhone(context, phone) }
                            }
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
                                        queueCandidates = apiService.getQueueCandidates()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
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
                    } else if (isDeliveryPhase) {
                        PhaseCard(
                            title = "Phase 2: Delivery",
                            address = orderDetails?.delivery_address ?: "Address unavailable",
                            recipientName = orderDetails?.recipient_name,
                            recipientPhone = orderDetails?.recipient_phone,
                            buttonText = "Navigate to Delivery",
                            onNavigate = { 
                                navigateToLocation(
                                    context, 
                                    orderDetails?.delivery_address ?: "",
                                    orderDetails?.delivery_lat,
                                    orderDetails?.delivery_lng
                                ) 
                            },
                            onCallRecipient = {
                                orderDetails?.recipient_phone?.let { phone -> callPhone(context, phone) }
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (normalizedStatus != "ARRIVED_AT_DELIVERY") {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        try {
                                            apiService.updateOrderStatus(orderId, mapOf("status" to "ARRIVED_AT_DELIVERY"))
                                            orderStatus = "ARRIVED_AT_DELIVERY"
                                            Toast.makeText(context, "Delivery Protocol Initiated", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                                        } finally { isLoading = false }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm Arrival at Destination")
                            }
                        }

                        if (normalizedStatus == "ARRIVED_AT_DELIVERY") {
                            Card(
                                onClick = { 
                                    podUri?.let { cameraLauncher.launch(it) }
                                        ?: Toast.makeText(context, "Storage error: Cannot launch camera", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = if (deliveryPhotoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(if (deliveryPhotoUri != null) "Photo Captured ✅" else "Capture Proof of Delivery")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Recipient Signature", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.Start))
                            SignaturePad(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .height(150.dp)
                                    .background(Color.White, shape = MaterialTheme.shapes.small),
                                onSignatureCaptured = { /* We will capture on complete */ }
                            )

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
                                            val location = try {
                                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                                            } catch (e: SecurityException) { null }
                                            
                                            if (podFile.exists()) {
                                                val compressedFile = ImageUtils.compressFile(context, podFile)
                                                val requestFile = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
                                                val body = MultipartBody.Part.createFormData("document", compressedFile.name, requestFile)
                                                val uploadRes = apiService.uploadOrderPhoto(body)
                                                val photoUrl = uploadRes["url"] ?: ""

                                                apiService.verifyDelivery(
                                                    orderId, 
                                                    VerifyCodeRequest(
                                                        code = deliveryCode, 
                                                        delivery_photo_url = photoUrl,
                                                        lat = location?.latitude,
                                                        lng = location?.longitude,
                                                        device_timestamp = System.currentTimeMillis()
                                                    )
                                                )
                                                orderStatus = "DELIVERED"
                                                showRatingDialog = true
                                            } else {
                                                Toast.makeText(context, "Please capture a proof photo first.", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Throwable) {
                                            Toast.makeText(context, "Process Failure: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
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
                    } else if (isQueued) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Mission Queued", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("You have another active mission in progress. This mission will begin as soon as your current mission is completed.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
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
                        onOrderCompleted()
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

@Composable
fun PhaseCard(
    title: String,
    address: String,
    recipientName: String? = null,
    recipientPhone: String? = null,
    buttonText: String,
    onNavigate: () -> Unit,
    onCallRecipient: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = address, 
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!recipientName.isNullOrBlank() || !recipientPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!recipientName.isNullOrBlank()) {
                            Text(
                                text = "Recipient: $recipientName", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (!recipientPhone.isNullOrBlank()) {
                            Text(
                                text = recipientPhone, 
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    if (onCallRecipient != null && !recipientPhone.isNullOrBlank()) {
                        IconButton(
                            onClick = onCallRecipient,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call Recipient",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigate, 
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun callPhone(context: Context, phoneNumber: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot launch phone dialer", Toast.LENGTH_SHORT).show()
    }
}

fun navigateToLocation(context: Context, address: String, lat: Double? = null, lng: Double? = null) {
    val uri = if (lat != null && lng != null && lat != 0.0) {
        Uri.parse("google.navigation:q=$lat,$lng")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    }
    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
    mapIntent.setPackage("com.google.android.apps.maps")
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        val fallbackUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
        context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
    }
}
