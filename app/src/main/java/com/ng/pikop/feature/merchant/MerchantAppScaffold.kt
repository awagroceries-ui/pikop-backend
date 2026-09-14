package com.ng.pikop.feature.merchant

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.feature.auth.AccountScreen
import com.ng.pikop.feature.wallet.WalletScreen
import kotlinx.coroutines.launch

@Composable
fun MerchantAppScaffold(
    rootNavController: NavHostController,
    userEmail: String,
    userName: String,
    userRole: String,
    tokenManager: TokenManager
) {
    val nestedNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                    selected = currentDestination == "dashboard",
                    onClick = { nestedNavController.navigate("dashboard") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                    label = { Text("Inventory") },
                    selected = currentDestination == "inventory",
                    onClick = { nestedNavController.navigate("inventory") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                    label = { Text("Orders") },
                    selected = currentDestination == "orders",
                    onClick = { nestedNavController.navigate("orders") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Wallet, contentDescription = null) },
                    label = { Text("Wallet") },
                    selected = currentDestination == "wallet",
                    onClick = { nestedNavController.navigate("wallet") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                    label = { Text("Store") },
                    selected = currentDestination == "account",
                    onClick = { nestedNavController.navigate("account") { launchSingleTop = true } }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nestedNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                // For now, load MerchantPortalScreen (or a customized dashboard subset)
                MerchantPortalScreen(
                    onAddItem = { type, id -> rootNavController.navigate("add_edit_product/$type/$id") },
                    onEditItem = { type, mId, pId -> rootNavController.navigate("add_edit_product/$type/$mId?productId=$pId") },
                    onCreateBatch = { rootNavController.navigate("bulk_dispatch") },
                    onBack = { }
                )
            }
            composable("inventory") {
                // Usually an Inventory view. For simplicity, we can route back to Portal or build a specific view.
                // Since Portal already has tabs, we just rely on Portal for now, but theoretically this would be the Products Tab.
                MerchantPortalScreen(
                    onAddItem = { type, id -> rootNavController.navigate("add_edit_product/$type/$id") },
                    onEditItem = { type, mId, pId -> rootNavController.navigate("add_edit_product/$type/$mId?productId=$pId") },
                    onCreateBatch = { rootNavController.navigate("bulk_dispatch") },
                    onBack = { }
                )
            }
            composable("orders") {
                MerchantOrdersScreen(
                    onNavigateToOrderDetails = { orderId -> rootNavController.navigate("active_order/$orderId") }
                )
            }
            composable("wallet") {
                WalletScreen(
                    onBack = { nestedNavController.popBackStack() }, 
                    isFulfiller = false,
                    onNavigateToWithdrawal = { rootNavController.navigate("withdrawal") }
                )
            }
            composable("account") {
                AccountScreen(
                    userEmail = userEmail,
                    userName = userName,
                    userRole = userRole,
                    referralCode = "",
                    kycStatus = null,
                    onNavigateToSupport = { rootNavController.navigate("support_hub") },
                    onNavigateToAddresses = { },
                    onNavigateToProfile = { rootNavController.navigate("profile_edit") },
                    onNavigateToNotifications = { rootNavController.navigate("notifications_settings") },
                    onNavigateToRecipients = { rootNavController.navigate("recipients_mgmt") },
                    onNavigateToSessions = { rootNavController.navigate("session_mgmt") },
                    onNavigateToCorporate = { rootNavController.navigate("corporate_dashboard") },
                    onNavigateToGrowth = { rootNavController.navigate("growth_rewards") },
                    onNavigateToMerchant = { }, // already in it
                    onNavigateToMerchantRegistration = { },
                    onNavigateToTerms = { rootNavController.navigate("terms_viewer/false") },
                    onNavigateToPrivacy = { rootNavController.navigate("privacy_policy") },
                    onLogout = {
                        scope.launch {
                            tokenManager.clearTokens()
                            rootNavController.navigate("user_type_selection") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
