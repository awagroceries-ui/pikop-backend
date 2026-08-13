package com.ng.pikop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseApp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.feature.auth.*
import com.ng.pikop.feature.fulfiller.ActiveOrderScreen
import com.ng.pikop.feature.fulfiller.FulfillerDashboardScreen
import com.ng.pikop.feature.fulfiller.FulfillerOrdersScreen
import com.ng.pikop.feature.fulfiller.KycUploadScreen
import com.ng.pikop.feature.order.*
import com.ng.pikop.feature.wallet.WalletScreen
import com.ng.pikop.ui.theme.PikopTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("PikopLifecycle", "MainActivity onCreate (Process ID: ${android.os.Process.myPid()})")
        enableEdgeToEdge()
        setContent {
            PikopTheme {
                PikopAppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("PikopLifecycle", "MainActivity onDestroy")
    }
}

@Composable
fun PikopAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    
    val userEmail by tokenManager.userEmail.collectAsState(initial = null)
    val userRole by tokenManager.userRole.collectAsState(initial = null)
    val referralCode by tokenManager.referralCode.collectAsState(initial = null)
    val accessToken by tokenManager.accessToken.collectAsState(initial = null)

    // Handle Intent Deep-linking
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        if (navigateTo == "chat" && accessToken != null) {
            navController.navigate("main") 
        }
    }

    // Push Token Registration (Safe)
    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            try {
                val apps = FirebaseApp.getApps(context)
                if (apps.isNotEmpty()) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            scope.launch {
                                try {
                                    val api = ApiService.create(tokenManager)
                                    api.updateFCMToken(mapOf("token" to token))
                                } catch (e: Exception) {}
                            }
                        }
                    }
                } else {
                    // Fallback: Try to initialize if somehow missed
                    try {
                        FirebaseApp.initializeApp(context)
                    } catch (e: Exception) {}
                }
            } catch (e: Throwable) {}
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onAnimationFinished = {
                if (accessToken != null) {
                    navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                } else {
                    navController.navigate("user_type_selection") { popUpTo("splash") { inclusive = true } }
                }
            })
        }

        composable("user_type_selection") {
            UserTypeSelectionScreen(onRoleSelected = { role ->
                if (role == "LOGIN") navController.navigate("login")
                else navController.navigate("signup/$role")
            })
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("main") { popUpTo("login") { inclusive = true } }
                },
                onGoToSignup = { navController.navigate("user_type_selection") }
            )
        }

        composable("signup/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "CUSTOMER"
            SignupScreen(
                role = role,
                onSignupSuccess = { email, userRole ->
                    navController.navigate("email_otp/$email/$userRole")
                },
                onViewTerms = { navController.navigate("terms_viewer/${role == "FULFILLER"}") },
                onViewPrivacy = { navController.navigate("privacy_policy") }
            )
        }

        composable("email_otp/{email}/{role}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: "CUSTOMER"
            EmailOtpScreen(email = email, onVerificationSuccess = { navController.navigate("terms/$role") })
        }

        composable("terms/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "CUSTOMER"
            TermsScreen(
                onAccept = { navController.navigate("main") { popUpTo("user_type_selection") { inclusive = true } } },
                showFulfillerTerms = role == "FULFILLER"
            )
        }

        composable("main") {
            MainAppScaffold(
                navController = navController,
                userEmail = userEmail ?: "",
                userRole = userRole ?: "CUSTOMER",
                referralCode = referralCode ?: "",
                tokenManager = tokenManager
            )
        }

        // Sub-flows (Full screen)
        composable("active_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ActiveOrderScreen(orderId = orderId, onOrderCompleted = { navController.popBackStack() })
        }
        composable("track_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            TrackOrderScreen(orderId = orderId, pickup = LatLng(6.5244, 3.3792), delivery = LatLng(6.4281, 3.4219))
        }
        composable("order_quote") {
            OrderQuoteScreen(userEmail = userEmail ?: "", onOrderComplete = { navController.popBackStack() })
        }
        composable("kyc_upload") {
            KycUploadScreen(onBack = { navController.popBackStack() })
        }
        composable("chat/{conversationId}") { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(conversationId = conversationId, userId = 1, userRole = userRole ?: "CUSTOMER", onBack = { navController.popBackStack() })
        }
        composable("privacy_policy") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
        composable("terms_viewer/{showFulfillerTerms}") { backStackEntry ->
            val showFulfillerTerms = backStackEntry.arguments?.getString("showFulfillerTerms")?.toBoolean() ?: false
            TermsScreen(onAccept = { navController.popBackStack() }, isViewer = true, showFulfillerTerms = showFulfillerTerms)
        }
        composable("profile_edit") { ProfileEditScreen(onBack = { navController.popBackStack() }) }
        composable("notifications_settings") { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("recipients_mgmt") { RecipientManagementScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    userEmail: String,
    userRole: String,
    referralCode: String,
    tokenManager: TokenManager
) {
    val nestedNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentDestination == "home",
                    onClick = { nestedNavController.navigate("home") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Missions") },
                    selected = currentDestination == "history",
                    onClick = { nestedNavController.navigate("history") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Wallet, contentDescription = null) },
                    label = { Text("Wallet") },
                    selected = currentDestination == "wallet",
                    onClick = { nestedNavController.navigate("wallet") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Menu") },
                    selected = currentDestination == "account",
                    onClick = { nestedNavController.navigate("account") { launchSingleTop = true } }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = nestedNavController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                if (userRole == "FULFILLER") {
                    FulfillerDashboardScreen(
                        userEmail = userEmail,
                        onAcceptOffer = { id -> navController.navigate("active_order/$id") },
                        onGoToWallet = { nestedNavController.navigate("wallet") },
                        onGoToKyc = { navController.navigate("kyc_upload") },
                        onGoToAbout = { nestedNavController.navigate("account") },
                        onLogout = {} 
                    )
                } else {
                    OrdersDashboardScreen(
                        userEmail = userEmail,
                        onNewDelivery = { navController.navigate("order_quote") },
                        onTrackOrder = { id -> navController.navigate("track_order/$id") },
                        onManageAddresses = { nestedNavController.navigate("account") },
                        onGoToWallet = { nestedNavController.navigate("wallet") },
                        onGoToAbout = { nestedNavController.navigate("account") },
                        onLogout = {}
                    )
                }
            }
            composable("history") {
                if (userRole == "FULFILLER") {
                    FulfillerOrdersScreen(onBack = { nestedNavController.popBackStack() })
                } else {
                    OrdersDashboardScreen(userEmail, {}, { id -> navController.navigate("track_order/$id") }, {}, {}, {}, {})
                }
            }
            composable("wallet") {
                WalletScreen(onBack = { nestedNavController.popBackStack() }, isFulfiller = userRole == "FULFILLER")
            }
            composable("account") {
                AccountScreen(
                    userEmail = userEmail,
                    userRole = userRole,
                    referralCode = referralCode,
                    onNavigateToSupport = {
                        scope.launch {
                            try {
                                val api = ApiService.create(tokenManager)
                                val conv = api.getOrCreateSupportConversation()
                                navController.navigate("chat/${conv.id}")
                            } catch (e: Exception) {}
                        }
                    },
                    onNavigateToAddresses = { 
                        // Implementation for addresses flow
                    },
                    onNavigateToProfile = { navController.navigate("profile_edit") },
                    onNavigateToNotifications = { navController.navigate("notifications_settings") },
                    onNavigateToRecipients = { navController.navigate("recipients_mgmt") },
                    onLogout = {
                        scope.launch {
                            tokenManager.clearTokens()
                            navController.navigate("user_type_selection") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
