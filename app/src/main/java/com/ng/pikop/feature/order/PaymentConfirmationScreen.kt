package com.ng.pikop.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.delay

@Composable
fun PaymentConfirmationScreen(
    reference: String,
    onConfirmed: (String) -> Unit,
    onFailed: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    
    var status by remember { mutableStateOf("confirming") } // confirming, success, failed
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var orderId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reference) {
        var attempts = 0
        val maxAttempts = 10 // Increase attempts for slow webhooks
        
        while (attempts < maxAttempts) {
            try {
                val res = apiService.verifyPayment(reference)
                if (res["success"] == true) {
                    val rawId = res["order_id"]
                    // Handle Gson numeric parsing (comes back as Double)
                    orderId = if (rawId is Double) rawId.toInt().toString() else rawId?.toString()
                    
                    status = "success"
                    delay(1500)
                    onConfirmed(orderId ?: "")
                    return@LaunchedEffect
                } else {
                    // Transaction found but not success yet, or not found.
                    // If it's a hard rejection from Paystack, we fail.
                    if (res["status"] == "failed") {
                        status = "failed"
                        errorMessage = "Payment failed at gateway."
                        return@LaunchedEffect
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PaymentConfirm", "Verify attempt ${attempts+1} failed: ${e.message}")
            }
            
            attempts++
            if (attempts < maxAttempts) delay(3000) // Wait 3s before retry
        }
        
        status = "failed"
        errorMessage = "We couldn't confirm your payment automatically. Please check your missions list or contact support."
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (status) {
                "confirming" -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Confirming your order...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Please don't close the app. We are activating your mission now.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                "success" -> {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50), 
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Payment Successful!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text("Your mission is now active. Redirecting to tracking...", textAlign = TextAlign.Center)
                }
                "failed" -> {
                    Icon(
                        Icons.Default.Error, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.error, 
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Verification Delayed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        errorMessage ?: "Something went wrong.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { onFailed(errorMessage ?: "Payment confirmation timed out") }) {
                        Text("Back to Home")
                    }
                }
            }
        }
    }
}
