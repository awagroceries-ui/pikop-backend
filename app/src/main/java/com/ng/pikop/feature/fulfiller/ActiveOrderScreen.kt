package com.ng.pikop.feature.fulfiller

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.VerifyCodeRequest
import kotlinx.coroutines.launch

@Composable
fun ActiveOrderScreen(orderId: String, onOrderCompleted: () -> Unit) {
    // In a real app, you'd fetch the order details from the API here
    // For alpha, we'll assume some static data or pass it in
    var orderStatus by remember { mutableStateOf("MATCHED") } // MATCHED -> PICKED_UP -> DELIVERED
    var pickupCode by remember { mutableStateOf("") }
    var deliveryCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Active Delivery", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Order ID: #$orderId", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))

            // Pickup Phase
            if (orderStatus == "MATCHED") {
                PhaseCard(
                    title = "Phase 1: Pickup",
                    address = "User's Pickup Address", // Replace with real data
                    buttonText = "Navigate to Pickup",
                    onNavigate = { navigateToAddress(context, "User's Pickup Address") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pickupCode,
                    onValueChange = { pickupCode = it },
                    label = { Text("Enter 4-digit Pickup Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val response = apiService.verifyPickup(orderId, VerifyCodeRequest(pickupCode))
                                orderStatus = "PICKED_UP"
                                Toast.makeText(context, "Pickup Verified!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid Pickup Code", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && pickupCode.length == 4
                ) {
                    Text("Verify Pickup")
                }
            }

            // Delivery Phase
            if (orderStatus == "PICKED_UP") {
                PhaseCard(
                    title = "Phase 2: Delivery",
                    address = "User's Delivery Address", // Replace with real data
                    recipientPhone = "+234 812 345 6789", // Visible now!
                    buttonText = "Navigate to Delivery",
                    onNavigate = { navigateToAddress(context, "User's Delivery Address") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = deliveryCode,
                    onValueChange = { deliveryCode = it },
                    label = { Text("Enter 4-digit Delivery Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                val response = apiService.verifyDelivery(orderId, VerifyCodeRequest(deliveryCode))
                                orderStatus = "DELIVERED"
                                showRatingDialog = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid Delivery Code", Toast.LENGTH_SHORT).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && deliveryCode.length == 4
                ) {
                    Text("Complete Delivery")
                }
            }
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            onDismiss = { 
                showRatingDialog = false
                onOrderCompleted()
            },
            onSubmit = { rating ->
                coroutineScope.launch {
                    try {
                        apiService.rateCustomer(orderId, com.ng.pikop.core.network.RatingRequest(rating))
                    } catch (e: Exception) {}
                    showRatingDialog = false
                    onOrderCompleted()
                }
            }
        )
    }
}

@Composable
fun PhaseCard(
    title: String,
    address: String,
    recipientPhone: String? = null,
    buttonText: String,
    onNavigate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = address, style = MaterialTheme.typography.bodyLarge)
            
            if (recipientPhone != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Recipient: $recipientPhone", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onNavigate, modifier = Modifier.fillMaxWidth()) {
                Text(buttonText)
            }
        }
    }
}

fun navigateToAddress(context: Context, address: String) {
    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
    }
}
