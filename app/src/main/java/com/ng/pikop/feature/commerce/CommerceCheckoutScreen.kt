package com.ng.pikop.feature.commerce

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommerceCheckoutScreen(
    itemId: String,
    itemType: String,
    navController: NavController,
    useCart: Boolean = false,
    onSuccess: () -> Unit,
    onCelebration: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var item by remember { mutableStateOf<DiscoveryItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var walletBalance by remember { mutableStateOf(0.0) }
    var scheduledAt by remember { mutableStateOf<String?>(null) }
    var isSchedulingEnabled by remember { mutableStateOf(false) }
    var selectedDateTime by remember { mutableStateOf<Calendar?>(null) }
    
    var selectedPaymentMethod by remember { mutableStateOf("CARD") } // "CARD", "COD", "WALLET"

    // Delivery Location State
    val deliveryAddress by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("delivery_address")
        ?.observeAsState("") ?: remember { mutableStateOf("") }
    
    val deliveryLat by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Double>("delivery_lat")
        ?.observeAsState(0.0) ?: remember { mutableStateOf(0.0) }
        
    val deliveryLng by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Double>("delivery_lng")
        ?.observeAsState(0.0) ?: remember { mutableStateOf(0.0) }

    LaunchedEffect(itemId) {
        try {
            isLoading = true
            val response = apiService.getDiscovery() 
            item = response.data.find { it.id == itemId && it.item_type == itemType }
            
            val walletRes = apiService.getWalletInfo()
            walletBalance = walletRes.balance ?: 0.0
        } catch (_: Exception) {
            Toast.makeText(context, "Error loading item", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    fun showDateTimePicker() {
        val current = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val date = Calendar.getInstance()
                date.set(year, month, dayOfMonth)
                
                android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        date.set(Calendar.MINUTE, minute)
                        date.set(Calendar.SECOND, 0)
                        
                        if (date.after(Calendar.getInstance())) {
                            selectedDateTime = date
                            scheduledAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(date.time)
                        } else {
                            Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show()
                        }
                    },
                    current.get(Calendar.HOUR_OF_DAY),
                    current.get(Calendar.MINUTE),
                    false
                ).show()
            },
            current.get(Calendar.YEAR),
            current.get(Calendar.MONTH),
            current.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    // Promo Code State
    var promoCodeInput by remember { mutableStateOf("") }
    var activePromo by remember { mutableStateOf<PromoValidationResponse?>(null) }
    var isValidatingPromo by remember { mutableStateOf(false) }

    // Dynamic Price Logic
    val itemBasePrice = if (useCart) com.ng.pikop.core.cart.CartManager.totalAmount else (item?.price ?: 0.0)
    val deliveryFee = if (deliveryAddress.isBlank()) 0.0 else 1200.0
    val platformFee = if (selectedPaymentMethod == "COD") itemBasePrice * 0.10 else 0.0
    val baseTotal = itemBasePrice + deliveryFee + platformFee

    val promoDiscount = if (activePromo == null) 0.0
        else if ((activePromo?.value ?: 0.0) >= 100.0) baseTotal
        else if (activePromo?.discount_type == "fixed") activePromo?.value ?: 0.0
        else minOf(deliveryFee * ((activePromo?.value ?: 0.0) / 100.0), deliveryFee)

    val totalAmount = maxOf(0.0, baseTotal - promoDiscount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item Header
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://api.pikop.com.ng${item!!.photo_url}",
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(item!!.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(item!!.vendor_name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("₦${"%,.2f".format(item!!.price)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text("Delivery Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                OutlinedCard(
                    onClick = { navController.navigate("map_address_search/Delivery Address/delivery") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (deliveryAddress.isBlank()) "Select Delivery Address" else deliveryAddress,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Choose where you want your item delivered", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Delivery Schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isSchedulingEnabled, onClick = { isSchedulingEnabled = false; scheduledAt = null }, label = { Text("Deliver Now") })
                    FilterChip(selected = isSchedulingEnabled, onClick = { isSchedulingEnabled = true }, label = { Text("Schedule for Later") })
                }

                if (isSchedulingEnabled) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                val displayText = selectedDateTime?.let { 
                                    SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault()).format(it.time)
                                } ?: "Select Date & Time"
                                Text(displayText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                
                                Button(onClick = { showDateTimePicker() }) {
                                    Text("Change")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        OutlinedCard(
                            modifier = Modifier.width(150.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (selectedPaymentMethod == "CARD") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selectedPaymentMethod == "CARD") 2.dp else 1.dp,
                                color = if (selectedPaymentMethod == "CARD") MaterialTheme.colorScheme.primary else Color.LightGray
                            ),
                            onClick = { selectedPaymentMethod = "CARD" }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = if (selectedPaymentMethod == "CARD") MaterialTheme.colorScheme.primary else Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pay Now", fontWeight = FontWeight.Bold)
                                Text("(Card)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }

                    if (walletBalance > 0) {
                        item {
                            val canAfford = totalAmount <= walletBalance
                            OutlinedCard(
                                modifier = Modifier.width(150.dp),
                                enabled = canAfford,
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (selectedPaymentMethod == "WALLET") MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selectedPaymentMethod == "WALLET") 2.dp else 1.dp,
                                    color = if (selectedPaymentMethod == "WALLET") MaterialTheme.colorScheme.primary else Color.LightGray
                                ),
                                onClick = { selectedPaymentMethod = "WALLET" }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (selectedPaymentMethod == "WALLET") MaterialTheme.colorScheme.primary else Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("My Wallet", fontWeight = FontWeight.Bold)
                                    Text("₦${walletBalance.toInt()}", style = MaterialTheme.typography.bodySmall, color = if (canAfford) Color.Gray else MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    if (item!!.accepts_cod) {
                        item {
                            OutlinedCard(
                                modifier = Modifier.width(150.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (selectedPaymentMethod == "COD") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (selectedPaymentMethod == "COD") 2.dp else 1.dp,
                                    color = if (selectedPaymentMethod == "COD") MaterialTheme.colorScheme.primary else Color.LightGray
                                ),
                                onClick = { selectedPaymentMethod = "COD" }
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = if (selectedPaymentMethod == "COD") MaterialTheme.colorScheme.primary else Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Pay on Delivery", fontWeight = FontWeight.Bold)
                                    Text("(Cash)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Promo Code Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it.uppercase() },
                        label = { Text("Promo Code (e.g. TESTER100)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isValidatingPromo = true
                                try {
                                    val res = apiService.validatePromoCode(mapOf("code" to promoCodeInput, "amount" to baseTotal.toString()))
                                    if (res.promo_id != null) {
                                        activePromo = res
                                        Toast.makeText(context, res.message ?: "Promo Applied!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid Coupon Code", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_SHORT).show()
                                } finally { isValidatingPromo = false }
                            }
                        },
                        enabled = !isValidatingPromo && promoCodeInput.isNotBlank()
                    ) {
                        if (isValidatingPromo) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        else Text("Apply")
                    }
                }

                if (activePromo != null) {
                    Text(
                        "Promo Active: -₦${"%,.2f".format(promoDiscount)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommercePriceRow("Item Price", itemBasePrice)
                        CommercePriceRow("Delivery Fee", deliveryFee)
                        CommercePriceRow("Escrow service fee", platformFee)
                        if (promoDiscount > 0) {
                            CommercePriceRow("Discount", -promoDiscount)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Payable", fontWeight = FontWeight.Bold)
                            Text("₦${"%,.2f".format(totalAmount)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        scope.launch {
                            isPlacingOrder = true
                            try {
                                val response = apiService.initializeCommerceOrder(CommerceOrderRequest(
                                    items = if (useCart) com.ng.pikop.core.cart.CartManager.items.map { mapOf("id" to it.item.id, "quantity" to it.quantity, "type" to it.item.item_type) } else null,
                                    item_id = if (useCart) null else itemId,
                                    item_type = if (useCart) null else itemType,
                                    delivery_address = deliveryAddress,
                                    lat = deliveryLat,
                                    lng = deliveryLng,
                                    payment_method = if (totalAmount == 0.0) "FREE" else if (selectedPaymentMethod == "WALLET") "WALLETPAY" else selectedPaymentMethod,
                                    scheduled_at = scheduledAt,
                                    promo_id = activePromo?.promo_id
                                ))

                                if (selectedPaymentMethod == "CARD" && !response.authorization_url.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.authorization_url))
                                    context.startActivity(intent)
                                    onCelebration()
                                    onSuccess() // Navigates back to main. Real flow would verify via webhook/intent
                                } else if ((selectedPaymentMethod == "COD" || selectedPaymentMethod == "WALLET") && !response.order_id.isNullOrBlank()) {
                                    Toast.makeText(context, if (selectedPaymentMethod == "WALLET") "Payment Successful!" else "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                                    onCelebration()
                                    navController.navigate("track_order/${response.order_id}") {
                                        popUpTo("main") { inclusive = false }
                                    }
                                } else {
                                    Toast.makeText(context, "Unexpected response from server.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                val errorMsg = ErrorUtils.parseError(e)
                                Toast.makeText(context, "Payment Failed: $errorMsg", Toast.LENGTH_LONG).show()
                            } finally {
                                isPlacingOrder = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPlacingOrder && deliveryAddress.isNotBlank()
                ) {
                    if (isPlacingOrder) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Pay & Complete Order")
                }
            }
        }
    }
}

@Composable
fun CommercePriceRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text("₦${"%,.2f".format(amount)}", fontWeight = FontWeight.Medium)
    }
}
