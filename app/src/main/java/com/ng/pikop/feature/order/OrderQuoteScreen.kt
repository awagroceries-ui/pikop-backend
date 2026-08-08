package com.ng.pikop.feature.order

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.SavedAddress
import com.ng.pikop.core.network.QuoteRequest
import com.ng.pikop.core.network.FareBreakdown
import com.ng.pikop.core.network.CreateOrderRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun OrderQuoteScreen(userEmail: String, onOrderComplete: (String) -> Unit) {
    var pickupAddress by remember { mutableStateOf("") }
    var pickupLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    var deliveryAddress by remember { mutableStateOf("") }
    var deliveryLatLng by remember { mutableStateOf<LatLng?>(null) }
    
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    
    var description by remember { mutableStateOf("") }
    var itemPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var itemPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    // UI State for Map Picker
    var showMapPickerFor by remember { mutableStateOf<String?>(null) } // "pickup" or "delivery"

    // Recipient Details
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var quoteResult by remember { mutableStateOf<FareBreakdown?>(null) }
    var quoteId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        itemPhotoUri = uri
    }

    // Fetch Saved Addresses
    LaunchedEffect(Unit) {
        try {
            savedAddresses = apiService.getSavedAddresses()
        } catch (e: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.pikop_logo),
            contentDescription = "Pikop Logo",
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Request a Delivery",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        if (savedAddresses.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Quick Select", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                savedAddresses.forEach { addr ->
                    AssistChip(
                        onClick = {
                            if (pickupAddress.isBlank()) {
                                pickupAddress = addr.address_text
                                pickupLatLng = LatLng(addr.lat, addr.lng)
                            } else {
                                deliveryAddress = addr.address_text
                                deliveryLatLng = LatLng(addr.lat, addr.lng)
                            }
                        },
                        label = { Text(addr.label) },
                        leadingIcon = {
                            Icon(
                                if (addr.label.lowercase() == "home") Icons.Default.Home else Icons.Default.Work,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AddressAutocompleteField(
            label = "Pickup Location",
            value = pickupAddress,
            onValueChange = { address, latLng ->
                pickupAddress = address
                if (latLng != null) pickupLatLng = latLng
            },
            onOpenMap = { showMapPickerFor = "pickup" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        AddressAutocompleteField(
            label = "Delivery Location",
            value = deliveryAddress,
            onValueChange = { address, latLng ->
                deliveryAddress = address
                if (latLng != null) deliveryLatLng = latLng
            },
            onOpenMap = { showMapPickerFor = "delivery" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Item Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // Item Photo Step
        Card(
            onClick = { photoLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (itemPhotoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (itemPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = if (itemPhotoUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (itemPhotoUri != null) "Item Photo Attached" else "Take Photo of Item",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Fulfillers need to see the item before accepting.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Recipient Details", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Recipient Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = recipientPhone,
            onValueChange = { recipientPhone = it },
            label = { Text("Recipient Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (quoteResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Fare: ₦${quoteResult!!.total_fare}", style = MaterialTheme.typography.titleLarge)
                    Text("Size Tier: ${quoteResult!!.size_tier}")
                    Text("Locked Until: ${quoteResult!!.fare_locked_until}")
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (quoteId == null) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = apiService.getQuote(
                                QuoteRequest(
                                    pickup_address = pickupAddress,
                                    delivery_address = deliveryAddress,
                                    item_description = description,
                                    pickup_lat = pickupLatLng?.latitude ?: 0.0,
                                    pickup_lng = pickupLatLng?.longitude ?: 0.0,
                                    delivery_lat = deliveryLatLng?.latitude ?: 0.0,
                                    delivery_lng = deliveryLatLng?.longitude ?: 0.0
                                )
                            )
                            quoteId = response.quote_id
                            quoteResult = response.fare_breakdown
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to get quote"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && pickupAddress.isNotBlank() && deliveryAddress.isNotBlank() && itemPhotoUri != null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Get Fare Quote")
                }
            }
        } else {
            Button(
                onClick = {
                    if (activity != null && quoteResult != null) {
                        val amountInKobo = (quoteResult!!.total_fare * 100).toLong()
                        CheckoutHelper.startCardCheckout(
                            activity = activity,
                            email = userEmail,
                            amountInKobo = amountInKobo,
                            onSuccess = { transaction ->
                                coroutineScope.launch {
                                    isLoading = true
                                    Toast.makeText(context, "Finalizing mission...", Toast.LENGTH_LONG).show()
                                    
                                    try {
                                        // 1. Upload item photo first
                                        val file = getFileFromUri(context, itemPhotoUri!!)
                                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                        val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
                                        val uploadRes = apiService.uploadOrderPhoto(body)
                                        itemPhotoUrl = uploadRes["url"]

                                        // 2. Finalize order
                                        val success = finalizeOrderAfterPayment(
                                            apiService = apiService,
                                            quoteId = quoteId!!,
                                            paymentReference = transaction.reference,
                                            recipientName = recipientName,
                                            recipientPhone = recipientPhone,
                                            notes = if (notes.isBlank()) null else notes,
                                            pLat = pickupLatLng?.latitude ?: 0.0,
                                            pLng = pickupLatLng?.longitude ?: 0.0,
                                            dLat = deliveryLatLng?.latitude ?: 0.0,
                                            dLng = deliveryLatLng?.longitude ?: 0.0,
                                            itemPhotoUrl = itemPhotoUrl!!,
                                            pSummary = pickupAddress.take(50),
                                            dSummary = deliveryAddress.take(50)
                                        )
                                        if (success) {
                                            onOrderComplete(transaction.reference)
                                        } else {
                                            errorMessage = "Payment confirmed, but mission deployment failed. Contact Pikop Support."
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Deployment Error: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            onError = { error ->
                                errorMessage = error.localizedMessage ?: "Payment failed"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && recipientName.isNotBlank() && recipientPhone.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Pay ₦${quoteResult!!.total_fare} & Deploy Mission")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Map Picker Sheet
    if (showMapPickerFor != null) {
        MapPickerSheet(
            onDismiss = { showMapPickerFor = null },
            onLocationSelected = { address, latLng ->
                if (showMapPickerFor == "pickup") {
                    pickupAddress = address
                    pickupLatLng = latLng
                } else {
                    deliveryAddress = address
                    deliveryLatLng = latLng
                }
                showMapPickerFor = null
            }
        )
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "item_temp_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

suspend fun finalizeOrderAfterPayment(
    apiService: ApiService,
    quoteId: String,
    paymentReference: String,
    recipientName: String,
    recipientPhone: String,
    notes: String?,
    pLat: Double,
    pLng: Double,
    dLat: Double,
    dLng: Double,
    itemPhotoUrl: String,
    pSummary: String,
    dSummary: String
): Boolean {
    return try {
        val request = CreateOrderRequest(
            quote_id = quoteId,
            payment_method = "card",
            recipient_name = recipientName,
            recipient_phone = recipientPhone,
            notes = notes,
            pickup_lat = pLat,
            pickup_lng = pLng,
            delivery_lat = dLat,
            delivery_lng = dLng,
            item_photo_url = itemPhotoUrl,
            pickup_display_summary = pSummary,
            delivery_display_summary = dSummary
        )
        val response = apiService.createOrder(request)
        response.status == "SEARCHING" || response.status == "MATCHED"
    } catch (e: Exception) {
        false
    }
}
