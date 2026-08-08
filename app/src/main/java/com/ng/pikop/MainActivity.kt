package com.ng.pikop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.LatLng
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.feature.auth.*
import com.ng.pikop.feature.fulfiller.ActiveOrderScreen
import com.ng.pikop.feature.fulfiller.FulfillerDashboardScreen
import com.ng.pikop.feature.fulfiller.FulfillerOrdersScreen
import com.ng.pikop.feature.fulfiller.KycUploadScreen
import com.ng.pikop.feature.order.OrderQuoteScreen
import com.ng.pikop.feature.order.OrdersDashboardScreen
import com.ng.pikop.feature.order.SavedAddressesScreen
import com.ng.pikop.feature.order.TrackOrderScreen
import com.ng.pikop.feature.wallet.WalletScreen
import com.ng.pikop.ui.theme.PikopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PikopTheme {
                PikopAppNavigation()
            }
        }
    }
}

@Composable
fun PikopAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    val userEmail by tokenManager.userEmail.collectAsState(initial = null)
    val userRole by tokenManager.userRole.collectAsState(initial = null)
    val accessToken by tokenManager.accessToken.collectAsState(initial = null)

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onAnimationFinished = {
                if (accessToken != null) {
                    val target = if (userRole == "FULFILLER") "fulfiller_dashboard" else "orders_dashboard"
                    navController.navigate(target) {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("user_type_selection") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            })
        }

        composable("user_type_selection") {
            UserTypeSelectionScreen(onRoleSelected = { role ->
                if (role == "LOGIN") {
                    navController.navigate("login")
                } else {
                    navController.navigate("signup/$role")
                }
            })
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    val target = if (role == "FULFILLER") "fulfiller_dashboard" else "orders_dashboard"
                    navController.navigate(target) {
                        popUpTo("login") { inclusive = true }
                    }
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
                onViewTerms = {
                    navController.navigate("terms_viewer/${role == "FULFILLER"}")
                },
                onViewPrivacy = {
                    navController.navigate("privacy_policy")
                }
            )
        }

        composable("email_otp/{email}/{role}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: "CUSTOMER"
            EmailOtpScreen(
                email = email,
                onVerificationSuccess = { 
                    navController.navigate("terms/$role") 
                }
            )
        }

        composable("terms/{role}") { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "CUSTOMER"
            TermsScreen(
                onAccept = { 
                    val target = if (role == "FULFILLER") "fulfiller_dashboard" else "orders_dashboard"
                    navController.navigate(target) {
                        popUpTo("user_type_selection") { inclusive = true }
                    }
                },
                showFulfillerTerms = role == "FULFILLER"
            )
        }

        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable("about_pikop/{isFulfiller}") { backStackEntry ->
            val isFulfiller = backStackEntry.arguments?.getString("isFulfiller")?.toBoolean() ?: false
            AboutPikopScreen(
                onBack = { navController.popBackStack() },
                onViewTerms = { role -> navController.navigate("terms_viewer/$role") },
                onViewPrivacy = { navController.navigate("privacy_policy") },
                isFulfiller = isFulfiller
            )
        }

        composable("terms_viewer/{showFulfillerTerms}") { backStackEntry ->
            val showFulfillerTerms = backStackEntry.arguments?.getString("showFulfillerTerms")?.toBoolean() ?: false
            TermsScreen(
                onAccept = { navController.popBackStack() }, 
                isViewer = true,
                showFulfillerTerms = showFulfillerTerms
            )
        }

        composable("orders_dashboard") {
            OrdersDashboardScreen(
                onNewDelivery = { navController.navigate("order_quote") },
                onTrackOrder = { orderId -> navController.navigate("track_order/$orderId") },
                onManageAddresses = { navController.navigate("saved_addresses") },
                onGoToWallet = { navController.navigate("wallet/false") },
                onGoToAbout = { navController.navigate("about_pikop/false") }
            )
        }

        composable("saved_addresses") {
            SavedAddressesScreen(onBack = { navController.popBackStack() })
        }

        composable("wallet/{isFulfiller}") { backStackEntry ->
            val isFulfiller = backStackEntry.arguments?.getString("isFulfiller")?.toBoolean() ?: false
            WalletScreen(
                onBack = { navController.popBackStack() },
                isFulfiller = isFulfiller
            )
        }

        composable("order_quote") {
            Column {
                OrderQuoteScreen(
                    userEmail = userEmail ?: "",
                    onOrderComplete = { reference ->
                        navController.navigate("orders_dashboard") {
                            popUpTo("orders_dashboard") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("fulfiller_dashboard") {
            FulfillerDashboardScreen(
                onAcceptOffer = { orderId ->
                    navController.navigate("active_order/$orderId")
                },
                onGoToWallet = {
                    navController.navigate("wallet/true")
                },
                onGoToKyc = {
                    navController.navigate("kyc_upload")
                },
                onGoToAbout = {
                    navController.navigate("about_pikop/true")
                }
            )
        }

        composable("kyc_upload") {
            KycUploadScreen(onBack = { navController.popBackStack() })
        }

        composable("fulfiller_history") {
            FulfillerOrdersScreen(onBack = { navController.popBackStack() })
        }

        composable("active_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ActiveOrderScreen(
                orderId = orderId,
                onOrderCompleted = {
                    navController.navigate("fulfiller_dashboard") {
                        popUpTo("fulfiller_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("track_order/{orderId}") { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            TrackOrderScreen(
                orderId = orderId,
                pickup = LatLng(6.5244, 3.3792),
                delivery = LatLng(6.4281, 3.4219)
            )
        }
    }
}
