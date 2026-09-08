package com.ng.pikop.feature.order

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderQuoteScreen(
    navController: NavHostController,
    userEmail: String,
    userName: String = "",
    userPhone: String = "",
    onOrderComplete: (String) -> Unit,
    onNavigateToPayment: (url: String, quoteId: String, pLat: Double, pLng: Double, dLat: Double, dLng: Double, itemUrl: String, pSum: String, dSum: String, rName: String, rPhone: String, notes: String?, promoId: String?) -> Unit
) {
    var pickupAddress by rememberSaveable { mutableStateOf("") }
    var pickupLat by rememberSaveable { mutableStateOf(0.0) }
    var pickupLng by rememberSaveable { mutableStateOf(0.0) }
    
    var deliveryAddress by rememberSaveable { mutableStateOf("") }
    var deliveryLat by rememberSaveable { mutableStateOf(0.0) }
    var deliveryLng by rememberSaveable { mutableStateOf(0.0) }

    val pickupLatLng = if (pickupLat != 0.0) LatLng(pickupLat, pickupLng) else null
    val deliveryLatLng = if (deliveryLat != 0.0) LatLng(deliveryLat, deliveryLng) else null
    
    // Use IDs to track which result we've already consumed
    var lastPickupResultId by rememberSaveable { mutableStateOf("") }
    var lastDeliveryResultId by rememberSaveable { mutableStateOf("") }

    val pAddrRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("pickup_address", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val pLatRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("pickup_lat", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val pLngRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("pickup_lng", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    val dAddrRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("delivery_address", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val dLatRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("delivery_lat", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val dLngRes by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("delivery_lng", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(pAddrRes, pLatRes, pLngRes) {
        if (pAddrRes != null && pLatRes != null && pLngRes != null) {
            val resultId = "${pAddrRes}_${pLatRes}_${pLngRes}"
            if (resultId != lastPickupResultId) {
                pickupAddress = pAddrRes!!
                pickupLat = pLatRes!!
                pickupLng = pLngRes!!
                lastPickupResultId = resultId
                // We don't remove from handle immediately to avoid StateFlow race conditions
                // instead we rely on lastPickupResultId to avoid duplicates.
            }
        }
    }

    LaunchedEffect(dAddrRes, dLatRes, dLngRes) {
        if (dAddrRes != null && dLatRes != null && dLngRes != null) {
            val resultId = "${dAddrRes}_${dLatRes}_${dLngRes}"
            if (resultId != lastDeliveryResultId) {
                deliveryAddress = dAddrRes!!
                deliveryLat = dLatRes!!
                deliveryLng = dLngRes!!
                lastDeliveryResultId = resultId
            }
        }
    }

    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var corporateAccounts by remember { mutableStateOf<List<CorporateAccount>>(emptyList()) }
    var selectedCorporateAccount by remember { mutableStateOf<CorporateAccount?>(null) }
    
    var description by rememberSaveable { mutableStateOf("") }
    var itemPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    var isSecurePay by rememberSaveable { mutableStateOf(false) }
    var itemPrice by rememberSaveable { mutableStateOf("") }
    var initiatorRole by rememberSaveable { mutableStateOf("PAYER") }
    var sellerPhone by rememberSaveable { mutableStateOf("") }

    var promoCode by rememberSaveable { mutableStateOf("") }
    var activePromo by remember { mutableStateOf<PromoValidationResponse?>(null) }
    
    var recipientName by rememberSaveable { mutableStateOf("") }
    var recipientPhone by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(userName, userPhone) {
        if (recipientName.isNullOrBlank()) {
            recipientName = userName ?: ""
        }
        if (recipientPhone.isNullOrBlank()) {
            recipientPhone = userPhone ?: ""
        }
    }

    var quoteResult by remember { mutableStateOf<QuoteResponse?>(null) }
    var quoteId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    val photoLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> itemPhotoUri = uri }

    LaunchedEffect(Unit) {
        try {
            val response = apiService.getSavedAddresses()
            savedAddresses = response.addresses
            corporateAccounts = apiService.getMyCorporateAccounts()
        } catch (e: Exception) {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(id = R.drawable.pikop_logo), contentDescription = "Pikop Logo", modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Request a Delivery", 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.onBackground, 
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Locations
            LocationInput(
                label = "Pickup Location",
                address = pickupAddress,
                onClick = { navController.navigate("map_address_search/Pickup/pickup") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LocationInput(
                label = "Delivery Location",
                address = deliveryAddress,
                onClick = { navController.navigate("map_address_search/Delivery/delivery") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description, 
                onValueChange = { description = it }, 
                label = { Text("Item Description", color = MaterialTheme.colorScheme.primary) }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                onClick = { photoLauncher.launch("image/*") }, 
                modifier = Modifier.fillMaxWidth(), 
                colors = CardDefaults.cardColors(containerColor = if (itemPhotoUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (itemPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto, contentDescription = null, tint = if (itemPhotoUri != null) MaterialTheme.colorScheme.primary else Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = if (itemPhotoUri != null) "Photo Attached" else "Take Photo of Item", style = MaterialTheme.typography.titleSmall, color = if (itemPhotoUri != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                }
            }

            // Secure Pay Section
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSecurePay) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Secure Pay Protection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Escrow-protected payment for your item.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = isSecurePay, onCheckedChange = { isSecurePay = it })
                    }

                    if (isSecurePay) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Are you the Buyer or the Seller?", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = initiatorRole == "PAYER", onClick = { initiatorRole = "PAYER" }, label = { Text("I am the Buyer") })
                            FilterChip(selected = initiatorRole == "SELLER", onClick = { initiatorRole = "SELLER" }, label = { Text("I am the Seller") })
                        }

                        OutlinedTextField(
                            value = itemPrice,
                            onValueChange = { if (it.all { char -> char.isDigit() }) itemPrice = it },
                            label = { Text("Item Price (₦)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )

                        if (initiatorRole == "PAYER") {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = sellerPhone,
                                onValueChange = { sellerPhone = it },
                                label = { Text("Seller's Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                            )
                        }
                    }
                }
            }

            // Promo Code
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    label = { Text("Promo Code", color = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.weight(1f),
                    enabled = activePromo == null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val total = quoteResult?.total_fare ?: 0.0
                                activePromo = apiService.validatePromoCode(mapOf(
                                    "code" to promoCode,
                                    "amount" to total.toString()
                                ))
                                Toast.makeText(context, activePromo?.message ?: "Code Applied", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid Code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = promoCode.isNotBlank() && activePromo == null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, 
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Apply")
                }
            }
            activePromo?.let { 
                Text("Discount: ${if(it.discount_type == "flat") "₦${it.value ?: 0.0}" else "${it.value ?: 0.0}%"}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }

            // Billing Method
            if (corporateAccounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Billing Method", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.onBackground, 
                    modifier = Modifier.align(Alignment.Start),
                    fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedCorporateAccount == null, onClick = { selectedCorporateAccount = null }, label = { Text("Personal") })
                    corporateAccounts.forEach { acc ->
                        FilterChip(selected = selectedCorporateAccount?.id == acc.id, onClick = { selectedCorporateAccount = acc }, label = { Text(acc.company_name ?: "Company") })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Recipient Details", 
                style = MaterialTheme.typography.titleMedium, 
                color = MaterialTheme.colorScheme.onBackground, 
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = recipientName, 
                onValueChange = { recipientName = it }, 
                label = { Text("Recipient Name", color = MaterialTheme.colorScheme.primary) }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = recipientPhone, 
                onValueChange = { recipientPhone = it }, 
                label = { Text("Recipient Phone", color = MaterialTheme.colorScheme.primary) }, 
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (quoteResult != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    val result = quoteResult!!
                    val total = result.total_fare ?: 0.0
                    val size = result.size_tier ?: "MEDIUM"
                    val promo = activePromo
                    val discount = if (promo == null) 0.0 else if (promo.discount_type == "flat") promo.value ?: 0.0 else total * ((promo.value ?: 0.0) / 100)
                    
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (isSecurePay) {
                            SummaryLine("Item Price", "₦${result.item_price ?: 0.0}")
                            SummaryLine("Delivery Fee", "₦${result.delivery_fee ?: 0.0}")
                            if (result.platform_fee_amount != null && result.platform_fee_amount!! > 0 && result.fee_payer == "PAYER") {
                                SummaryLine("Secure Pay Fee (Initiator)", "₦${result.platform_fee_amount}")
                            }
                        } else {
                            SummaryLine("Delivery Fee", "₦$total")
                        }

                        if (discount > 0) {
                            SummaryLine("Discount", "-₦$discount", color = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Payable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("₦${total - discount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (isSecurePay && result.fee_payer == "SELLER") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Platform fee (₦${result.platform_fee_amount}) will be deducted from Seller's payout.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }

                        if (isSecurePay && result.payer_info?.type == "GUEST") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Recipient is not a Pikop user. They will receive an SMS link to pay.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (quoteId == null) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true; errorMessage = null
                            try {
                                val response = apiService.getQuote(
                                    QuoteRequest(
                                        pickup_address = pickupAddress, 
                                        delivery_address = deliveryAddress, 
                                        item_description = description, 
                                        pickup_lat = pickupLatLng?.latitude ?: 0.0, 
                                        pickup_lng = pickupLatLng?.longitude ?: 0.0, 
                                        delivery_lat = deliveryLatLng?.latitude ?: 0.0, 
                                        delivery_lng = deliveryLatLng?.longitude ?: 0.0,
                                        item_price = if (isSecurePay) itemPrice.toDoubleOrNull() ?: 0.0 else 0.0,
                                        initiator_role = if (isSecurePay) initiatorRole else "PAYER",
                                        recipient_phone = if (isSecurePay) recipientPhone else null
                                    )
                                )
                                if (response.success && response.quote_id != null) {
                                    quoteId = response.quote_id
                                    quoteResult = response
                                } else {
                                    errorMessage = "Failed to get valid quote"
                                }
                            } catch (e: Exception) { 
                                errorMessage = ErrorUtils.parseError(e)
                                Toast.makeText(context, "Quote Fetch Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && pickupAddress.isNotBlank() && deliveryAddress.isNotBlank() && itemPhotoUri != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, 
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), 
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Get Fare Quote", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val file = getFileFromUri(context, itemPhotoUri!!)
                                val uploadRes = apiService.uploadOrderPhoto(MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())))
                                val pUrl = uploadRes["url"] ?: ""
                                
                                val qId = quoteId ?: ""
                                val result = quoteResult
                                if (selectedCorporateAccount != null) {
                                    val success = finalizeOrderAfterPayment(
                                        apiService = apiService, 
                                        quoteId = qId, 
                                        corporateAccountId = selectedCorporateAccount!!.id, 
                                        promoId = activePromo?.promo_id, 
                                        paymentReference = "CORPORATE", 
                                        recipientName = recipientName, 
                                        recipientPhone = recipientPhone, 
                                        notes = notes, 
                                        pLat = pickupLatLng?.latitude ?: 0.0, 
                                        pLng = pickupLatLng?.longitude ?: 0.0, 
                                        dLat = deliveryLatLng?.latitude ?: 0.0, 
                                        dLng = deliveryLatLng?.longitude ?: 0.0, 
                                        itemPhotoUrl = pUrl, 
                                        pSummary = pickupAddress.take(50), 
                                        dSummary = deliveryAddress.take(50),
                                        quoteResult = result,
                                        sellerPhone = if (isSecurePay && initiatorRole == "PAYER") sellerPhone else null
                                    )
                                    if (success) {
                                        onOrderComplete("CORPORATE")
                                    } else {
                                        Toast.makeText(context, "Failed to finalize corporate order", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (result != null) {
                                    val total = result.total_fare ?: 0.0
                                    val promo = activePromo
                                    val discount = if (promo == null) 0.0 else if (promo.discount_type == "fixed") promo.value ?: 0.0 else total * ((promo.value ?: 0.0)/100)
                                    val amountToCharge = total - discount 

                                    // 100% DISCOUNT BYPASS
                                    if (amountToCharge <= 0) {
                                        val freePaymentRef = "FREE_${java.util.UUID.randomUUID()}"
                                        try {
                                            val request = CreateOrderRequest(
                                                quote_id = qId, 
                                                corporate_account_id = null, 
                                                promo_id = activePromo?.promo_id, 
                                                payment_method = "promo", 
                                                recipient_name = recipientName, 
                                                recipient_phone = recipientPhone, 
                                                notes = notes, 
                                                pickup_lat = pickupLatLng?.latitude ?: 0.0, 
                                                pickup_lng = pickupLatLng?.longitude ?: 0.0, 
                                                delivery_lat = deliveryLatLng?.latitude ?: 0.0, 
                                                delivery_lng = deliveryLatLng?.longitude ?: 0.0, 
                                                item_photo_url = pUrl, 
                                                pickup_display_summary = pickupAddress.take(50), 
                                                delivery_display_summary = deliveryAddress.take(50),
                                                payment_reference = freePaymentRef,
                                                item_price = result.item_price,
                                                seller_phone = if (isSecurePay && initiatorRole == "PAYER") sellerPhone else null
                                            )
                                            val response = apiService.createOrder(request)
                                            if (response.status == "SEARCHING" || response.status == "MATCHED" || response.status == "QUEUED" || response.status == "PAYMENT_CAPTURED") {
                                                Toast.makeText(context, "Mission Activated Successfully!", Toast.LENGTH_LONG).show()
                                                onOrderComplete("FREE")
                                            } else {
                                                Toast.makeText(context, "Server Error: Could not activate free mission", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            val detail = ErrorUtils.parseError(e)
                                            Toast.makeText(context, "Failed to activate free mission: $detail", Toast.LENGTH_LONG).show()
                                        }
                                        isLoading = false
                                        return@launch
                                    }

                                    try {
                                        val paymentInit = apiService.initializePayment(
                                            PaymentInitializationRequest(
                                                amount = amountToCharge, 
                                                email = userEmail,
                                                quote_id = qId,
                                                item_price = if (isSecurePay) itemPrice.toDoubleOrNull() ?: 0.0 else 0.0,
                                                delivery_fee = result.delivery_fee,
                                                platform_fee_amount = result.platform_fee_amount,
                                                fee_payer = result.fee_payer,
                                                seller_phone = if (isSecurePay && initiatorRole == "PAYER") sellerPhone else null
                                            )
                                        )
                                        val authUrl = paymentInit.authorization_url
                                        
                                        if (!authUrl.isNullOrBlank()) {
                                            onNavigateToPayment(
                                                authUrl!!,
                                                qId,
                                                pickupLatLng?.latitude ?: 0.0,
                                                pickupLatLng?.longitude ?: 0.0,
                                                deliveryLatLng?.latitude ?: 0.0,
                                                deliveryLatLng?.longitude ?: 0.0,
                                                pUrl,
                                                pickupAddress.take(50),
                                                deliveryAddress.take(50),
                                                recipientName,
                                                recipientPhone,
                                                notes,
                                                activePromo?.promo_id
                                            )
                                        } else {
                                            errorMessage = "Payment initialization failed: Missing gateway URL"
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = ErrorUtils.parseError(e)
                                        Toast.makeText(context, "Payment Error: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) { 
                                android.util.Log.e("PayDeploy", "Pay & Deploy failed", e)
                                errorMessage = ErrorUtils.parseError(e)
                                Toast.makeText(context, "Something went wrong: $errorMessage", Toast.LENGTH_SHORT).show()
                            } finally { isLoading = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isLoading && recipientName.isNotBlank() && recipientPhone.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, 
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp), 
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Pay & Deploy Mission", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SummaryLine(label: String, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LocationInput(label: String, address: String?, onClick: () -> Unit) {
    val displayAddress = address ?: ""
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (label.contains("Pickup")) Icons.Default.Circle else Icons.Default.Place, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(
                    text = displayAddress.ifBlank { "Search address..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (displayAddress.isBlank()) Color.LightGray else Color.Black,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File {
    val tempFile = File(context.cacheDir, "item_temp_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }
    return tempFile
}

suspend fun finalizeOrderAfterPayment(
    apiService: ApiService, quoteId: String, corporateAccountId: String?, promoId: String?, 
    paymentReference: String, recipientName: String, recipientPhone: String, notes: String?, 
    pLat: Double, pLng: Double, dLat: Double, dLng: Double, itemPhotoUrl: String, 
    pSummary: String, dSummary: String, quoteResult: QuoteResponse? = null,
    sellerPhone: String? = null
): Boolean {
    return try {
        // 1. Check if Webhook already created the mission (Preferred v3 Flow)
        var isAlreadyActive = false
        try {
            val check = apiService.getOrderByQuote(quoteId)
            if (check["success"] == true) {
                isAlreadyActive = true
            }
        } catch (e: Exception) {
            // HTTP 404 is EXPECTED if webhook has not run or for free missions. Continue to step 2.
            android.util.Log.d("PikopPayment", "by-quote check: order not active yet (${e.message})")
        }

        if (isAlreadyActive) {
            android.util.Log.d("PikopPayment", "Order already active via Webhook. Advancing.")
            return true
        }

        // 2. Fallback: Manually trigger activation if Webhook is delayed
        val request = CreateOrderRequest(
            quote_id = quoteId, 
            corporate_account_id = corporateAccountId, 
            promo_id = promoId, 
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
            delivery_display_summary = dSummary,
            payment_reference = paymentReference,
            item_price = quoteResult?.item_price,
            seller_phone = sellerPhone
        )
        val response = apiService.createOrder(request)
        response.status == "SEARCHING" || response.status == "MATCHED" || response.status == "QUEUED" || response.status == "PAYMENT_CAPTURED"
    } catch (e: Exception) { 
        android.util.Log.e("PikopPayment", "Finalization error: ${e.message}", e)
        false 
    }
}
