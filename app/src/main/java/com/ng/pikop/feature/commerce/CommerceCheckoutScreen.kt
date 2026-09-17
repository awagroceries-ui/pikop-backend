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
    onSuccess: () -> Unit,
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

    // Dynamic Price Logic (Initial heuristic for UI before final quote)
    val deliveryFee = if (deliveryAddress.isBlank()) 0.0 else 1200.0
    val platformFee = (item?.price ?: 0.0) * 0.10
    val totalAmount = (item?.price ?: 0.0) + deliveryFee + platformFee

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

                Spacer(modifier = Modifier.height(16.dp))
                Text("Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommercePriceRow("Item Price", item!!.price)
                        CommercePriceRow("Delivery Fee", deliveryFee)
                        CommercePriceRow("Escrow service fee", platformFee)
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
                                    item_id = itemId,
                                    item_type = itemType,
                                    delivery_address = deliveryAddress,
                                    lat = deliveryLat,
                                    lng = deliveryLng,
                                    payment_method = if (selectedPaymentMethod == "WALLET") "WALLETPAY" else selectedPaymentMethod
                                ))

                                if (selectedPaymentMethod == "CARD" && !response.authorization_url.isNullOrBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.authorization_url))
                                    context.startActivity(intent)
                                    onSuccess() // Navigates back to main. Real flow would verify via webhook/intent
                                } else if ((selectedPaymentMethod == "COD" || selectedPaymentMethod == "WALLET") && !response.order_id.isNullOrBlank()) {
                                    Toast.makeText(context, if (selectedPaymentMethod == "WALLET") "Payment Successful!" else "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
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
