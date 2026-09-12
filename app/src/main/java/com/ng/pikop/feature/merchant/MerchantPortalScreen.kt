package com.ng.pikop.feature.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                android.widget.Toast.makeText(context, "Delete failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
                    IconButton(onClick = { fetchDashboard() }) { Icon(Icons.Default.Refresh, null) }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab.intValue == 1 && merchantProfile.value != null) {
                FloatingActionButton(
                    onClick = { 
                        onAddItem(merchantProfile.value!!.type, merchantProfile.value!!.id) 
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Item")
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
                TabRow(selectedTabIndex = selectedTab.intValue) {
                    Tab(selected = selectedTab.intValue == 0, onClick = { selectedTab.intValue = 0 }, text = { Text("My Sales") })
                    Tab(selected = selectedTab.intValue == 1, onClick = { selectedTab.intValue = 1 }, text = { Text("Listings") })
                    Tab(selected = selectedTab.intValue == 2, onClick = { selectedTab.intValue = 2 }, text = { Text("Bulk") })
                }

                when (selectedTab.intValue) {
                    0 -> SalesTabContent(dashboardData.value?.sales ?: emptyList())
                    1 -> ListingsTabContent(
                        profile = merchantProfile.value,
                        products = dashboardData.value?.products ?: emptyList(),
                        onEdit = onEditItem,
                        onDelete = { type, id -> handleDeleteItem(type, id) }
                    )
                    2 -> BulkTabContent(dashboardData.value?.batches ?: emptyList())
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
            title = "No Marketplace Listings",
            description = "Register as a vendor and list your products on the Pikop Marketplace."
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onEdit = { 
                        if (profile != null) onEdit(profile.type, profile.id, product.id) 
                    },
                    onDelete = { onDelete(profile?.type ?: "vendor", product.id) }
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
            Text(text = "Price: ₦${"%,.2f".format(order.item_price ?: 0.0)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold)
                Text(text = "₦${"%,.2f".format(product.price)}", style = MaterialTheme.typography.bodySmall)
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
