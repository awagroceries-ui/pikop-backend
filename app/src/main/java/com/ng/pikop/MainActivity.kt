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
import com.ng.pikop.feature.auth.EmailOtpScreen
import com.ng.pikop.feature.auth.LoginScreen
import com.ng.pikop.feature.auth.SignupScreen
import com.ng.pikop.feature.auth.TermsScreen
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

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { navController.navigate("orders_dashboard") },
                onGoToSignup = { navController.navigate("signup") }
            )
        }
        composable("signup") {
            SignupScreen(onSignupSuccess = { email ->
                navController.navigate("email_otp/$email")
            })
        }
        composable("email_otp/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailOtpScreen(
                email = email,
                onVerificationSuccess = { navController.navigate("terms") }
            )
        }
        composable("terms") {
            TermsScreen(onAccept = { navController.navigate("orders_dashboard") })
        }
        composable("orders_dashboard") {
            OrdersDashboardScreen(
                onNewDelivery = { navController.navigate("order_quote") },
                onTrackOrder = { orderId -> navController.navigate("track_order/$orderId") },
                onManageAddresses = { navController.navigate("saved_addresses") },
                onGoToWallet = { navController.navigate("wallet/false") }
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
                Button(onClick = { navController.navigate("fulfiller_dashboard") }) {
                    Text("Switch to Fulfiller")
                }
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
