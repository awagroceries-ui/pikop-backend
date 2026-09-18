package com.ng.pikop.feature.merchant

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantPortalScreen(
    onAddItem: (String, String) -> Unit, // merchantType, merchantId
    onEditItem: (String, String, String) -> Unit, // merchantType, merchantId, productId
    onCreateBatch: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()
    
    val merchantProfile = remember { mutableStateOf<MerchantProfile?>(null) }
    val dashboardData = remember { mutableStateOf<MerchantDashboardData?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val selectedTab = remember { mutableIntStateOf(0) }

    fun fetchDashboard() {
        isLoading.value = true
        errorMessage.value = null
        scope.launch {
            try {
                val profileRes = apiService.getMerchantProfile()
                merchantProfile.value = profileRes.data
                
                val response = apiService.getMerchantDashboard()
                dashboardData.value = response.data
            } catch (e: Exception) {
                errorMessage.value = "Failed to load dashboard: ${e.message}"
                android.util.Log.e("MerchantUI", "Load error", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchDashboard()
    }

    fun handleDeleteItem(type: String, id: String) {
        scope.launch {
            try {
                if (type == "vendor") apiService.deleteProduct(id)
                else apiService.deleteMenuItem(id)
                fetchDashboard()
            } catch (e: Exception) {
                Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { selectedTab.intValue = 5 }) { Icon(Icons.Default.Settings, "Settings") }
                    IconButton(onClick = { fetchDashboard() }) { Icon(Icons.Default.Refresh, "Refresh") }
                }
            )
        },
        floatingActionButton = {
            val profile = merchantProfile.value
            if (profile != null && profile.status == "active") {
                if (selectedTab.intValue == 1) {
                    FloatingActionButton(
                        onClick = { onAddItem(profile.type, profile.id) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) { Icon(Icons.Default.Add, "Add Item") }
                } else if (selectedTab.intValue == 4) { 
                    FloatingActionButton(
                        onClick = onCreateBatch,
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) { Icon(Icons.Default.Layers, "Create Batch") }
                }
            }
        }
    ) { padding ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.value != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage.value!!, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { fetchDashboard() }) { Text("Retry") }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // 1. Verification Status Banner (v4.5)
                val profile = merchantProfile.value
                if (profile != null && profile.status != "active") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PendingActions, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Verification Pending", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Your business profile is under review. Listing items is disabled until approval.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else if (profile == null && !isLoading.value) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Business Setup Incomplete", fontWeight = FontWeight.Bold)
                            Text("Please complete your business registration to start selling.", style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { /* This case is handled in AccountScreen, but we could add a CTA here */ }) {
                                Text("Complete Setup")
                            }
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab.intValue,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = selectedTab.intValue == 0, onClick = { selectedTab.intValue = 0 }, text = { Text("Sales", fontSize = 12.sp) })
                    Tab(selected = selectedTab.intValue == 1, onClick = { selectedTab.intValue = 1 }, text = { Text("Items", fontSize = 12.sp) })
                    Tab(selected = selectedTab.intValue == 2, onClick = { selectedTab.intValue = 2 }, text = { Text("Insights", fontSize = 12.sp) })
                    Tab(selected = selectedTab.intValue == 3, onClick = { selectedTab.intValue = 3 }, text = { Text("Returns", fontSize = 12.sp) })
                    Tab(selected = selectedTab.intValue == 4, onClick = { selectedTab.intValue = 4 }, text = { Text("Bulk", fontSize = 12.sp) })
                }

                when (selectedTab.intValue) {
                    0 -> SalesTabContent(dashboardData.value?.sales ?: emptyList())
                    1 -> ListingsTabContent(
                        profile = merchantProfile.value,
                        products = dashboardData.value?.products ?: emptyList(),
                        onEdit = onEditItem,
                        onDelete = { type, id -> handleDeleteItem(type, id) }
                    )
                    2 -> MerchantAnalyticsScreen()
                    3 -> ReturnsTabContent(
                        onRefresh = { fetchDashboard() }
                    )
                    4 -> BulkTabContent(dashboardData.value?.batches ?: emptyList())
                    5 -> SettingsTabContent(
                        profile = merchantProfile.value,
                        onUpdateSettings = { acceptsCod, allowsReturns, windowDays ->
                            scope.launch {
                                try {
                                    apiService.updateMerchantSettings(mapOf(
                                        "accepts_cod" to (acceptsCod ?: merchantProfile.value?.accepts_cod ?: true),
                                        "allows_returns" to (allowsReturns ?: merchantProfile.value?.allows_returns ?: false),
                                        "return_window_days" to (windowDays ?: merchantProfile.value?.return_window_days ?: 7)
                                    ))
                                    fetchDashboard()
                                    Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SalesTabContent(sales: List<OrderDetailsResponse>) {
    if (sales.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Inventory2,
            title = "No Sales Yet",
            description = "Start using Secure Pay when selling items to track your orders here."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sales) { sale ->
                SaleItem(sale)
            }
        }
    }
}

@Composable
fun ListingsTabContent(
    profile: MerchantProfile?,
    products: List<Product>,
    onEdit: (String, String, String) -> Unit,
    onDelete: (String, String) -> Unit
) {
    if (products.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Storefront,
            title = "No Listings Found",
            description = profile?.let { 
                if (it.type == "vendor") "Register as a vendor and list your products on the Pikop Marketplace."
                else "Set up your cloud kitchen menu and start receiving food orders."
            } ?: "Start listing your items here."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onEdit = { 
                        if (profile != null) {
                            onEdit(product.merchant_type ?: profile.type, profile.id, product.id) 
                        }
                    },
                    onDelete = { onDelete(product.merchant_type ?: profile?.type ?: "vendor", product.id) }
                )
            }
        }
    }
}

@Composable
fun BulkTabContent(batches: List<MerchantBatch>) {
    if (batches.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Layers,
            title = "No Bulk Batches",
            description = "Create high-volume delivery batches using our Merchant API."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(batches) { batch ->
                BatchItem(batch)
            }
        }
    }
}

@Composable
fun SaleItem(order: OrderDetailsResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Order #${order.id?.takeLast(8)?.uppercase() ?: ""}", fontWeight = FontWeight.Bold)
                val statusColor = when(order.status) {
                    "DELIVERED" -> Color(0xFF4CAF50)
                    "RELEASED" -> Color(0xFF2196F3)
                    "DISPUTED" -> Color.Red
                    else -> MaterialTheme.colorScheme.primary
                }
                Badge(containerColor = statusColor) { Text(order.status?.replace("_", " ")?.uppercase() ?: "", color = Color.White) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = order.item_description ?: "Secure Pay Item", style = MaterialTheme.typography.bodyMedium)
            
            val price = order.item_price ?: 0.0
            val commission = order.merchant_commission_amount ?: 0.0
            val netPayout = price - commission

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Item Price", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₦${"%,.2f".format(price)}")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Marketplace Commission", color = MaterialTheme.colorScheme.error)
                Text("-₦${"%,.2f".format(commission)}", color = MaterialTheme.colorScheme.error)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Your Net Payout", fontWeight = FontWeight.Bold)
                Text("₦${"%,.2f".format(netPayout)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            
            if (order.escrow_status != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Payment: ${order.escrow_status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (product.active) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                if (!product.photo_url.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = "https://api.pikop.com.ng${product.photo_url}",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, null, tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name, 
                    fontWeight = FontWeight.Bold,
                    color = if (product.active) Color.Unspecified else Color.Gray
                )
                Text(text = "₦${"%,.2f".format(product.price)}", style = MaterialTheme.typography.bodySmall)
                if (!product.active) {
                    Text("HIDDEN / OUT OF STOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun BatchItem(batch: MerchantBatch) {
    val progress = if (batch.total_orders > 0) batch.processed_orders.toFloat() / batch.total_orders else 0f
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Batch: ${batch.name ?: batch.id.take(8).uppercase()}", fontWeight = FontWeight.Bold)
                Badge(containerColor = if (batch.status == "completed") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary) {
                    Text(batch.status.uppercase(), color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Created: ${batch.created_at.take(10)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${batch.processed_orders}/${batch.total_orders} Missions", style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun SettingsTabContent(
    profile: MerchantProfile?,
    onUpdateSettings: (Boolean?, Boolean?, Int?) -> Unit
) {
    if (profile == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Business Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accept Cash on Delivery", fontWeight = FontWeight.Bold)
                        Text(
                            "Allow customers to pay when they receive items. Funds are held in escrow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = profile.accepts_cod,
                        onCheckedChange = { onUpdateSettings(it, null, null) }
                    )
                }

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Marketplace Returns", fontWeight = FontWeight.Bold)
                        Text(
                            "Enable customers to request returns within your specified window.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = profile.allows_returns,
                        onCheckedChange = { onUpdateSettings(null, it, null) }
                    )
                }

                if (profile.allows_returns) {
                    var windowText by remember { mutableStateOf(profile.return_window_days.toString()) }
                    OutlinedTextField(
                        value = windowText,
                        onValueChange = { 
                            windowText = it
                            it.toIntOrNull()?.let { days -> onUpdateSettings(null, null, days) }
                        },
                        label = { Text("Return Window (Days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        }
        
        Text(
            "Note: The 10% platform fee for COD is paid by the customer, not deducted from your payout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ReturnsTabContent(onRefresh: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var returns by remember { mutableStateOf<List<ReturnResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetch() {
        scope.launch {
            isLoading = true
            try {
                val res = apiService.getMerchantReturnRequests()
                returns = res.data
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetch() }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (returns.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.KeyboardReturn,
            title = "No Return Requests",
            description = "Active returns from customers will appear here."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(returns) { ret ->
                ReturnRequestItem(
                    ret = ret,
                    onProcess = { status, notes, payer ->
                        scope.launch {
                            try {
                                apiService.processReturnRequest(ret.id, ProcessReturnRequest(status, notes, payer))
                                Toast.makeText(context, "Return $status", Toast.LENGTH_SHORT).show()
                                fetch()
                                onRefresh()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onConfirmReceipt = {
                        scope.launch {
                            try {
                                apiService.confirmReturnReceipt(ret.id)
                                Toast.makeText(context, "Refund processed", Toast.LENGTH_SHORT).show()
                                fetch()
                                onRefresh()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReturnRequestItem(
    ret: ReturnResponse,
    onProcess: (String, String?, String) -> Unit,
    onConfirmReceipt: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Return Request", fontWeight = FontWeight.Bold)
                Badge { Text(ret.status) }
            }
            
            Text("Customer: ${ret.customer_name ?: "Unknown"}")
            Text("Item: ${ret.item_description ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            
            Surface(color = Color.LightGray.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(ret.reason, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
            }

            if (ret.status == "PENDING") {
                var notes by remember { mutableStateOf("") }
                var feePayer by remember { mutableStateOf("CUSTOMER") }
                
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Internal Notes") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = feePayer == "MERCHANT", onCheckedChange = { feePayer = if (it) "MERCHANT" else "CUSTOMER" })
                    Text("I will cover return delivery fee", style = MaterialTheme.typography.labelSmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onProcess("APPROVED", notes, feePayer) }, modifier = Modifier.weight(1f)) {
                        Text("Approve")
                    }
                    Button(onClick = { onProcess("DECLINED", notes, "CUSTOMER") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Decline")
                    }
                }
            } else if (ret.status == "APPROVED") {
                Text("Awaiting item pickup/delivery...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            } else if (ret.status == "IN_TRANSIT") {
                Text("Item is on its way back to you.", style = MaterialTheme.typography.bodySmall, color = com.ng.pikop.ui.theme.PikopGreen)
                Button(onClick = onConfirmReceipt, modifier = Modifier.fillMaxWidth()) {
                    Text("Confirm Receipt & Refund")
                }
            }
        }
    }
}
