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
import com.ng.pikop.feature.chat.ChatScreen
import com.ng.pikop.feature.fulfiller.*
import com.ng.pikop.feature.order.*
import com.ng.pikop.feature.auth.*
import com.ng.pikop.feature.wallet.WalletScreen
import com.ng.pikop.ui.theme.PikopTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
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
    val userId by tokenManager.userId.collectAsState(initial = null)
    val userName by tokenManager.userName.collectAsState(initial = null)
    val userPhone by tokenManager.userPhone.collectAsState(initial = null)
    val userRole by tokenManager.userRole.collectAsState(initial = null)
    val isVerified by tokenManager.isVerified.collectAsState(initial = false)
    val referralCode by tokenManager.referralCode.collectAsState(initial = null)
    val accessToken by tokenManager.accessToken.collectAsState(initial = null)

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
    val activity = context as? ComponentActivity
    LaunchedEffect(activity?.intent) {
        val navigateTo = activity?.intent?.getStringExtra("navigate_to")
        val orderId = activity?.intent?.getStringExtra("order_id")
        
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
                        referralCode = referralCode
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
            TrackOrderScreen(orderId = orderId, pickup = LatLng(6.5244, 3.3792), delivery = LatLng(6.4281, 3.4219))
        }
        composable("order_quote") {
            OrderQuoteScreen(
                userEmail = userEmail ?: "",
                userName = userName ?: "",
                userPhone = userPhone ?: "",
                onOrderComplete = { navController.popBackStack() },
                onNavigateToPayment = { url, qId, pLat, pLng, dLat, dLng, itemUrl, pSum, dSum, rName, rPhone, notes, promoId ->
                    val encUrl = java.net.URLEncoder.encode(url, "UTF-8")
                    val encItemUrl = java.net.URLEncoder.encode(itemUrl, "UTF-8")
                    val encPSum = java.net.URLEncoder.encode(pSum, "UTF-8")
                    val encDSum = java.net.URLEncoder.encode(dSum, "UTF-8")
                    val encRName = java.net.URLEncoder.encode(rName, "UTF-8")
                    val encRPhone = java.net.URLEncoder.encode(rPhone, "UTF-8")
                    val encNotes = java.net.URLEncoder.encode(notes ?: "null", "UTF-8")
                    
                    // ULTRA-SAFE NAVIGATION: Pass ALL text and IDs as Query Parameters
                    // Path only contains the literal 'payment_webview'
                    navController.navigate(
                        "payment_webview?url=$encUrl&qId=$qId&pLat=$pLat&pLng=$pLng&dLat=$dLat&dLng=$dLng&itemUrl=$encItemUrl&pSum=$encPSum&dSum=$encDSum&rName=$encRName&rPhone=$encRPhone&notes=$encNotes&promoId=${promoId ?: "null"}"
                    )
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

        composable(
            route = "payment_webview?url={url}&qId={qId}&pLat={pLat}&pLng={pLng}&dLat={dLat}&dLng={dLng}&itemUrl={itemUrl}&pSum={pSum}&dSum={dSum}&rName={rName}&rPhone={rPhone}&notes={notes}&promoId={promoId}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("qId") { type = NavType.StringType },
                navArgument("pLat") { type = NavType.StringType },
                navArgument("pLng") { type = NavType.StringType },
                navArgument("dLat") { type = NavType.StringType },
                navArgument("dLng") { type = NavType.StringType },
                navArgument("itemUrl") { type = NavType.StringType },
                navArgument("pSum") { type = NavType.StringType },
                navArgument("dSum") { type = NavType.StringType },
                navArgument("rName") { type = NavType.StringType },
                navArgument("rPhone") { type = NavType.StringType },
                navArgument("notes") { type = NavType.StringType },
                navArgument("promoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("url") ?: "", "UTF-8")
            val qId = backStackEntry.arguments?.getString("qId") ?: ""
            val pLat = backStackEntry.arguments?.getString("pLat")?.toDouble() ?: 0.0
            val pLng = backStackEntry.arguments?.getString("pLng")?.toDouble() ?: 0.0
            val dLat = backStackEntry.arguments?.getString("dLat")?.toDouble() ?: 0.0
            val dLng = backStackEntry.arguments?.getString("dLng")?.toDouble() ?: 0.0
            val itemUrl = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("itemUrl") ?: "", "UTF-8")
            val pSum = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("pSum") ?: "", "UTF-8")
            val dSum = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("dSum") ?: "", "UTF-8")
            val rName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("rName") ?: "", "UTF-8")
            val rPhone = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("rPhone") ?: "", "UTF-8")
            val notes = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("notes") ?: "null", "UTF-8").let { if (it == "null") null else it }
            val promoId = backStackEntry.arguments?.getString("promoId")?.let { if (it == "null") null else it }

            PaymentWebView(
                url = url,
                onSuccess = { ref ->
                    scope.launch {
                        val api = ApiService.create(tokenManager)
                        val success = finalizeOrderAfterPayment(api, qId, null, promoId, ref, rName, rPhone, notes, pLat, pLng, dLat, dLng, itemUrl, pSum, dSum)
                        if (success) {
                            navController.navigate("main") {
                                popUpTo("order_quote") { inclusive = true }
                            }
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
