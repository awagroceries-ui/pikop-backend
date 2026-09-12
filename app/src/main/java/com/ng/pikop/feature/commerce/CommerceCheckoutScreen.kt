package com.ng.pikop.feature.commerce

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
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
                                    lng = deliveryLng
                                ))
                                
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.authorization_url))
                                context.startActivity(intent)
                                onSuccess()
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
