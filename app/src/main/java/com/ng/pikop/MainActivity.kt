package com.ng.pikop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.FirebaseApp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.feature.auth.*
import com.ng.pikop.feature.chat.*
import com.ng.pikop.feature.fulfiller.*
import com.ng.pikop.feature.order.*
import com.ng.pikop.feature.wallet.WalletScreen
import com.ng.pikop.ui.theme.PikopTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val _intentFlow = MutableStateFlow<Intent?>(null)
    val intentFlow = _intentFlow.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            android.util.Log.d("PikopLifecycle", "MainActivity onCreate - START (Process: ${android.os.Process.myPid()})")
            enableEdgeToEdge()
            
            // Initial intent
            _intentFlow.value = intent

            setContent {
                PikopTheme {
                    SideEffect {
                        android.util.Log.d("PikopLifecycle", "MainActivity - First Frame Rendered")
                    }
                    PikopAppNavigation(intentFlow = intentFlow)
                }
            }
            android.util.Log.d("PikopLifecycle", "MainActivity onCreate - SUCCESS")
        } catch (e: Throwable) {
            android.util.Log.e("PikopLifecycle", "CRITICAL CRASH IN ONCREATE: ${e.message}", e)
            // Show toast if possible or just crash with log
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _intentFlow.value = intent
        android.util.Log.d("PikopLifecycle", "MainActivity onNewIntent: ${intent.data}")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("PikopLifecycle", "MainActivity onDestroy")
    }
}

@Composable
fun PikopAppNavigation(intentFlow: kotlinx.coroutines.flow.StateFlow<Intent?>) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    
    val incomingIntent by intentFlow.collectAsState()
    val userEmail by tokenManager.userEmail.collectAsState(initial = null)
    val userId by tokenManager.userId.collectAsState(initial = null)
    val userName by tokenManager.userName.collectAsState(initial = null)
    val userPhone by tokenManager.userPhone.collectAsState(initial = null)
    val userRole by tokenManager.userRole.collectAsState(initial = null)
    val isVerified by tokenManager.isVerified.collectAsState(initial = false)
    val referralCode by tokenManager.referralCode.collectAsState(initial = null)
    val kycStatus by tokenManager.kycStatus.collectAsState(initial = null)
    val accessToken by tokenManager.accessToken.collectAsState(initial = null)

    // Global Session Monitor
    LaunchedEffect(Unit) {
        tokenManager.sessionEvents.collect { event ->
            if (event == TokenManager.SessionEvent.EXPIRED) {
                tokenManager.clearTokens()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
                android.widget.Toast.makeText(context, "Session expired. Please sign in again.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Redirect to OTP if not verified (Strict Gating)
    LaunchedEffect(accessToken, isVerified) {
        if (accessToken != null && !isVerified) {
            // Check current destination to avoid infinite loop
            val current = navController.currentDestination?.route
            if (current != null && !current.startsWith("email_otp") && current != "splash" && !current.startsWith("terms")) {
                navController.navigate("email_otp/$userEmail/$userRole") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // Notification Permission Request
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("PikopFCM", "Notification permission granted")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Handle Intent Deep-linking
    LaunchedEffect(incomingIntent) {
        val intent = incomingIntent ?: return@LaunchedEffect
        val dataUri = intent.data
        val navigateTo = intent.getStringExtra("navigate_to")
        val orderId = intent.getStringExtra("order_id")

        android.util.Log.d("PikopIntent", "Processing intent: $dataUri | navigate_to: $navigateTo")

        // 1. Handle Scheme-based Deep Links (e.g. pikop://payment/success)
        if (dataUri != null && dataUri.scheme == "pikop") {
            if (dataUri.host == "payment" && dataUri.path == "/success") {
                android.util.Log.d("PikopIntent", "Success deep-link detected. Clearing checkout state.")
                CheckoutHelper.activeQuote = null
                navController.navigate("main") {
                    popUpTo(0) { inclusive = true }
                }
                return@LaunchedEffect
            }
        }
        
        if (accessToken != null && navigateTo != null) {
            when (navigateTo) {
                "SUPPORT_CHAT", "chat" -> {
                    scope.launch {
                        try {
                            val api = ApiService.create(tokenManager)
                            val conv = api.getOrCreateSupportConversation()
                            navController.navigate("chat/${conv.id}")
                        } catch (_: Exception) {
                            navController.navigate("main")
                        }
                    }
                }
                "ORDER_CHAT" -> {
                    if (orderId != null) navController.navigate("order_chat/$orderId")
                    else navController.navigate("main")
                }
                "MISSION_OFFER" -> {
                    navController.navigate("main")
                }
                "ORDER_UPDATE" -> {
                    if (orderId != null) {
                        if (userRole == "FULFILLER") navController.navigate("active_order/$orderId")
                        else navController.navigate("track_order/$orderId")
                    } else {
                        navController.navigate("main")
                    }
                }
                else -> navController.navigate("main")
            }
        }
    }

    // Profile Auto-Sync & Push Token Registration
    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            // 1. Sync Profile Data
            scope.launch {
                try {
                    val api = ApiService.create(tokenManager)
                    val profile = api.getUserProfile()
                    tokenManager.saveTokens(
                        accessToken = accessToken!!,
                        refreshToken = tokenManager.refreshToken.first() ?: "",
                        userId = userId,
                        email = userEmail ?: "",
                        role = userRole ?: "CUSTOMER",
                        name = profile.full_name,
                        phone = profile.phone,
                        isVerified = isVerified,
                        referralCode = referralCode,
                        kycStatus = profile.kyc_status
                    )
                    android.util.Log.d("PikopSync", "Profile background sync complete")
                } catch (e: Exception) {
                    android.util.Log.e("PikopSync", "Profile sync failed: ${e.message}")
                }
            }

            // 2. FCM Token Registration
            try {
                val apps = FirebaseApp.getApps(context)
                if (apps.isNotEmpty()) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        android.util.Log.d("PikopFCM", "FCM Token retrieved: ${token.take(10)}...")
                        scope.launch {
                            try {
                                val api = ApiService.create(tokenManager)
                                val response = api.updateFCMToken(mapOf("token" to token))
                                android.util.Log.d("PikopFCM", "FCM Token registered: ${response.message}")
                            } catch (e: Exception) {
                                android.util.Log.e("PikopFCM", "FCM Token registration failed: ${e.message}")
                            }
                        }
                    } else {
                        android.util.Log.w("PikopFCM", "Fetching FCM registration token failed", task.exception)
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
                    if (isVerified) {
                        navController.navigate("main") { popUpTo("splash") { inclusive = true } }
                    } else {
                        navController.navigate("email_otp/$userEmail/$userRole") { popUpTo("splash") { inclusive = true } }
                    }
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
                onUnverified = { email, role ->
                    navController.navigate("email_otp/$email/$role")
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
            EmailOtpScreen(
                email = email, 
                onVerificationSuccess = { navController.navigate("terms/$role") },
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
                userName = userName ?: "",
                userPhone = userPhone ?: "",
                userRole = userRole ?: "CUSTOMER",
                referralCode = referralCode ?: "",
                kycStatus = kycStatus,
                tokenManager = tokenManager
            )
        }

        // Sub-flows (Full screen)
        composable("active_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ActiveOrderScreen(
                orderId = orderId, 
                onOrderCompleted = { navController.popBackStack() },
                onNavigateToChat = { id -> navController.navigate("order_chat/$id") }
            )
        }
        composable("track_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            TrackOrderScreen(orderId = orderId)
        }
        composable("order_quote") {
            OrderQuoteScreen(
                navController = navController,
                userEmail = userEmail ?: "",
                userName = userName ?: "",
                userPhone = userPhone ?: "",
                onOrderComplete = { navController.popBackStack() },
                onNavigateToPayment = { url, qId, pLat, pLng, dLat, dLng, itemUrl, pSum, dSum, rName, rPhone, notes, promoId ->
                    CheckoutHelper.activeQuote = CheckoutHelper.CheckoutData(
                        url = url,
                        quoteId = qId,
                        pLat = pLat,
                        pLng = pLng,
                        dLat = dLat,
                        dLng = dLng,
                        itemPhotoUrl = itemUrl,
                        pickupSummary = pSum,
                        deliverySummary = dSum,
                        recipientName = rName,
                        recipientPhone = rPhone,
                        notes = notes,
                        promoId = promoId
                    )
                    navController.navigate("payment_webview")
                }
            )
        }
        composable("kyc_upload") {
            KycUploadScreen(userEmail = userEmail ?: "", onBack = { navController.popBackStack() })
        }
        composable("chat/{conversationId}") { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(
                conversationId = conversationId, 
                userId = userId?.toIntOrNull() ?: 0, 
                userRole = userRole ?: "CUSTOMER", 
                onBack = { navController.popBackStack() }
            )
        }
        composable("order_chat/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ChatScreen(
                orderId = orderId, 
                userId = userId?.toIntOrNull() ?: 0, 
                userRole = userRole ?: "CUSTOMER", 
                onBack = { navController.popBackStack() }
            )
        }
        composable("privacy_policy") { PrivacyPolicyScreen(onBack = { navController.popBackStack() }) }
        composable("terms_viewer/{showFulfillerTerms}") { backStackEntry ->
            val showFulfillerTerms = backStackEntry.arguments?.getString("showFulfillerTerms")?.toBoolean() ?: false
            TermsScreen(onAccept = { navController.popBackStack() }, isViewer = true, showFulfillerTerms = showFulfillerTerms)
        }
        composable("profile_edit") { ProfileEditScreen(onBack = { navController.popBackStack() }) }
        composable("notifications_settings") { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("recipients_mgmt") { RecipientManagementScreen(onBack = { navController.popBackStack() }) }
        composable("session_mgmt") { SessionManagementScreen(onBack = { navController.popBackStack() }) }
        composable("corporate_dashboard") { CorporateDashboardScreen(onBack = { navController.popBackStack() }) }
        composable("insights") { InsightsScreen(onBack = { navController.popBackStack() }) }
        composable("saved_addresses") { 
            SavedAddressesScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            ) 
        }
        
        composable("map_address_search/{title}/{type}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Location"
            val type = backStackEntry.arguments?.getString("type") ?: "pickup"
            MapAddressSearchScreen(
                title = title,
                onBack = { navController.popBackStack() },
                onAddressSelected = { address, lat, lng ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("${type}_address", address)
                    navController.previousBackStackEntry?.savedStateHandle?.set("${type}_lat", lat)
                    navController.previousBackStackEntry?.savedStateHandle?.set("${type}_lng", lng)
                    navController.popBackStack()
                }
            )
        }

        composable("support_hub") {
            SupportHubScreen(
                onNavigateToFaqList = { category -> navController.navigate("faq_list/$category") },
                onNavigateToChat = {
                    scope.launch {
                        try {
                            val api = ApiService.create(tokenManager)
                            val conv = api.getOrCreateSupportConversation()
                            navController.navigate("chat/${conv.id}")
                        } catch (_: Exception) {}
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("faq_list/{category}") { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            FaqListScreen(
                category = category,
                onNavigateToDetail = { id -> navController.navigate("faq_detail/$id") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("faq_detail/{articleId}") { backStackEntry ->
            val articleId = backStackEntry.arguments?.getString("articleId") ?: ""
            FaqDetailScreen(
                articleId = articleId,
                onBack = { navController.popBackStack() }
            )
        }

        composable("payment_webview") {
            val data = CheckoutHelper.activeQuote
            if (data == null) {
                navController.popBackStack()
                return@composable
            }

            var isConfirming by remember { mutableStateOf(false) }
            val api = remember { ApiService.create(tokenManager) }

            // AUTO-RECOVERY & POLLING: Decoupled Source of Truth
            LaunchedEffect(data.quoteId) {
                while (true) {
                    delay(3000) // Poll every 3 seconds
                    try {
                        val res = api.getOrderByQuote(data.quoteId)
                        if (res["success"] == true) {
                            android.util.Log.d("PikopPayment", "POLLING SUCCESS: Order activated.")
                            CheckoutHelper.activeQuote = null
                            navController.navigate("main") {
                                popUpTo("order_quote") { inclusive = true }
                            }
                            break
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("PikopPayment", "Polling check skipped: ${e.message}")
                    }
                }
            }

            PaymentWebView(
                url = data.url,
                onSuccess = { reference: String, onResult: (Boolean) -> Unit ->
                    isConfirming = true
                    scope.launch {
                        try {
                            android.util.Log.d("PikopPayment", "CLIENT VERIFY: Triggering for $reference")
                            val verifyRes = api.verifyPayment(reference)
                            if (verifyRes["success"] == true) {
                                android.util.Log.d("PikopPayment", "VERIFY SUCCESS: Proceeding to main.")
                                CheckoutHelper.activeQuote = null
                                navController.navigate("main") {
                                    popUpTo("order_quote") { inclusive = true }
                                }
                            } else {
                                android.util.Log.w("PikopPayment", "VERIFY REJECTED: Status=${verifyRes["status"]}")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PikopPayment", "Verify API call failed: ${e.message}")
                        } finally {
                            onResult(true)
                        }
                    }
                },
                onCancel = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    userEmail: String,
    userName: String,
    userPhone: String,
    userRole: String,
    referralCode: String,
    kycStatus: String? = null,
    tokenManager: TokenManager
) {
    val nestedNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentDestination == "home",
                    onClick = { nestedNavController.navigate("home") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Missions") },
                    selected = currentDestination == "history",
                    onClick = { nestedNavController.navigate("history") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Wallet, contentDescription = null) },
                    label = { Text("Wallet") },
                    selected = currentDestination == "wallet",
                    onClick = { nestedNavController.navigate("wallet") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Menu") },
                    selected = currentDestination == "account",
                    onClick = { nestedNavController.navigate("account") { launchSingleTop = true } },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                        indicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    )
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
                        onGoToInsights = { navController.navigate("insights") },
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
                    userName = userName,
                    userRole = userRole,
                    referralCode = referralCode,
                    kycStatus = kycStatus,
                    onNavigateToSupport = { navController.navigate("support_hub") },
                    onNavigateToAddresses = { 
                        // Implementation for addresses flow
                    },
                    onNavigateToProfile = { navController.navigate("profile_edit") },
                    onNavigateToNotifications = { navController.navigate("notifications_settings") },
                    onNavigateToRecipients = { navController.navigate("recipients_mgmt") },
                    onNavigateToSessions = { navController.navigate("session_mgmt") },
                    onNavigateToCorporate = { navController.navigate("corporate_dashboard") },
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
