package com.ng.pikop.feature.order

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.OrderDetailsResponse
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    userEmail: String,
    userName: String,
    onNewDelivery: () -> Unit,
    onNavigateToFood: () -> Unit,
    onNavigateToGroceries: () -> Unit,
    onNavigateToShop: () -> Unit,
    onTrackOrder: (String) -> Unit,
    onNavigateToAcknowledgment: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    var activeOrders by remember { mutableStateOf<List<OrderDetailsResponse>>(emptyList()) }
    var incomingOrders by remember { mutableStateOf<List<OrderDetailsResponse>>(emptyList()) }
    var walletBalance by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    val fetchDashboard = {
        scope.launch {
            isLoading = true
            try {
                val orders = apiService.getUserOrders()
                activeOrders = orders.filter { it.status != "DELIVERED" && it.status != "CANCELLED" && it.status != "RECIPIENT_ABSENT" && it.status != "RELEASED" }
                
                // Fetch Incoming (for Acknowledgment)
                // Note: We reuse getUserOrders for now or add a specific getIncomingOrders if needed.
                // Assuming getUserOrders returns both sent and received for v3.
                incomingOrders = orders.filter { it.status == "PENDING_ACKNOWLEDGMENT" }

                val wallet = apiService.getWalletInfo()
                walletBalance = wallet.balance ?: 0.0
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.pikop_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pikop", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { fetchDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Welcome Header
            Text(
                text = "Hello, ${userName.split(" ").firstOrNull() ?: "there"}!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "What would you like to do today?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Incoming Deliveries Alert
            if (incomingOrders.isNotEmpty()) {
                val incoming = incomingOrders.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                    onClick = { onNavigateToAcknowledgment(incoming.id ?: "") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Incoming Delivery!", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Tap to confirm your address.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Active Mission Banner (Priority - v4.0)
            if (activeOrders.isNotEmpty()) {
                val mission = activeOrders.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = com.ng.pikop.ui.theme.PikopGreen),
                    onClick = { onTrackOrder(mission.id ?: "") },
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBike,
                            contentDescription = null,
                            tint = com.ng.pikop.ui.theme.PikopGold,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Active Mission: ${(mission.status ?: "ACTIVE").replace("_", " ")}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "Live tracking in progress...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // 2x2 Grid of Primary Modules (Rebranded v4.0)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Dispatch",
                    subtitle = "Instant delivery",
                    icon = Icons.Default.LocalShipping,
                    backgroundColor = com.ng.pikop.ui.theme.PikopGreen,
                    iconTint = Color.White,
                    onClick = onNewDelivery
                )
                PrimaryModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Food",
                    subtitle = "Hot meals",
                    icon = Icons.Default.Restaurant,
                    backgroundColor = com.ng.pikop.ui.theme.PikopOrange,
                    iconTint = Color.White,
                    onClick = onNavigateToFood
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Groceries",
                    subtitle = "Fresh produce",
                    icon = Icons.Default.ShoppingBasket,
                    backgroundColor = com.ng.pikop.ui.theme.PikopLemonGreen,
                    iconTint = com.ng.pikop.ui.theme.PikopGreen,
                    onClick = onNavigateToGroceries
                )
                PrimaryModuleCard(
                    modifier = Modifier.weight(1f),
                    title = "Shop",
                    subtitle = "General store",
                    icon = Icons.Default.ShoppingBag,
                    backgroundColor = com.ng.pikop.ui.theme.PikopGold,
                    iconTint = com.ng.pikop.ui.theme.PikopNearBlack,
                    onClick = onNavigateToShop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Secondary Grid Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ServiceButton(
                    modifier = Modifier.weight(1f),
                    title = "My Wallet",
                    subtitle = "₦${"%,.0f".format(walletBalance)}",
                    icon = Icons.Default.AccountBalanceWallet,
                    color = com.ng.pikop.ui.theme.PikopGrey,
                    onClick = onNavigateToWallet
                )
                ServiceButton(
                    modifier = Modifier.weight(1f),
                    title = "Saved Places",
                    subtitle = "Quick access",
                    icon = Icons.Default.Bookmark,
                    color = com.ng.pikop.ui.theme.PikopGrey,
                    onClick = onNavigateToAddresses
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ServiceButton(
                    modifier = Modifier.weight(1f),
                    title = "Support Hub",
                    subtitle = "Get help",
                    icon = Icons.Default.SupportAgent,
                    color = com.ng.pikop.ui.theme.PikopGrey,
                    onClick = onNavigateToSupport
                )
                ServiceButton(
                    modifier = Modifier.weight(1f),
                    title = "Settings",
                    subtitle = "Account info",
                    icon = Icons.Default.Settings,
                    color = com.ng.pikop.ui.theme.PikopGrey,
                    onClick = onNavigateToAddresses
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Helpful Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Your complete mission history can be found in the 'Missions' tab below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = com.ng.pikop.ui.theme.PikopGreen, modifier = Modifier.size(24.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 11.sp, color = com.ng.pikop.ui.theme.PikopDarkGrey)
            }
        }
    }
}

@Composable
fun PrimaryModuleCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(130.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = iconTint
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = iconTint.copy(alpha = 0.8f)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 12.dp)
                    .graphicsLayer(alpha = 0.25f),
                tint = iconTint
            )
        }
    }
}
