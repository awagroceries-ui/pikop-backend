package com.ng.pikop.feature.commerce

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ng.pikop.core.cart.CartManager
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.DiscoveryItem
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryStorefrontScreen(
    merchantId: String? = null,
    onViewCart: () -> Unit = {},
    onItemClick: (DiscoveryItem) -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var items by remember { mutableStateOf<List<DiscoveryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val categories = listOf("All", "Fresh Produce", "Dairy & Eggs", "Beverages", "Snacks", "Household")

    suspend fun fetchGroceries() {
        try {
            val location = try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            } catch (e: SecurityException) { null }

            val response = apiService.getDiscovery(
                lat = location?.latitude,
                lng = location?.longitude,
                item_type = "groceries",
                vendor_id = merchantId,
                category = if (selectedCategory == "All") null else selectedCategory,
                query = if (searchQuery.isBlank()) null else searchQuery
            )
            items = response.data
        } catch (e: Exception) {
            android.util.Log.e("GroceryStorefront", "Fetch error", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(selectedCategory, searchQuery) {
        isLoading = true
        fetchGroceries()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Order Groceries",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (CartManager.items.isNotEmpty()) {
                            IconButton(onClick = onViewCart) {
                                BadgedBox(
                                    badge = { Badge { Text("${CartManager.items.sumOf { it.quantity }}") } }
                                ) {
                                    Icon(Icons.Default.ShoppingCart, "View Cart")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search fresh items, drinks, or household") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            // Category Horizontal Scroll
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            } else if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalGroceryStore, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Text("No groceries found nearby.", color = Color.Gray)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = { "${it.id}_${it.item_type}" }) { item ->
                        DiscoveryItemCard(
                            item = item, 
                            onClick = { 
                                CartManager.clear()
                                CartManager.addItem(item)
                                onItemClick(item) 
                            }
                        )
                    }
                }
            }
        }

        // Unobstructed Bottom Cart Banner Bar
        if (CartManager.items.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clickable { onViewCart() },
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BadgedBox(
                            badge = { Badge { Text("${CartManager.items.sumOf { it.quantity }}") } }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("View Cart", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text("₦${"%,.2f".format(CartManager.totalAmount)}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Checkout", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
