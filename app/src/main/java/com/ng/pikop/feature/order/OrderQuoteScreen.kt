package com.ng.pikop.feature.order

import android.app.Activity
import co.paystack.android.Transaction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.QuoteRequest
import com.ng.pikop.core.network.FareBreakdown
import com.ng.pikop.core.network.CreateOrderRequest
import kotlinx.coroutines.launch

@Composable
fun OrderQuoteScreen(userEmail: String, onOrderComplete: (String) -> Unit) {
    var pickup by remember { mutableStateOf("") }
    var delivery by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Recipient Details
    var recipientName by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var quoteResult by remember { mutableStateOf<FareBreakdown?>(null) }
    var quoteId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Request a Delivery", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pickup,
            onValueChange = { pickup = it },
            label = { Text("Pickup Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = delivery,
            onValueChange = { delivery = it },
            label = { Text("Delivery Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Item Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Recipient Details", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Recipient Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = recipientPhone,
            onValueChange = { recipientPhone = it },
            label = { Text("Recipient Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        if (quoteResult != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Fare: ₦${quoteResult!!.total_fare}", style = MaterialTheme.typography.titleLarge)
                    Text("Size Tier: ${quoteResult!!.size_tier}")
                    Text("Locked Until: ${quoteResult!!.fare_locked_until}")
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (quoteId == null) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = apiService.getQuote(QuoteRequest(pickup, delivery, description))
                            quoteId = response.quote_id
                            quoteResult = response.fare_breakdown
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to get quote"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && pickup.isNotBlank() && delivery.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Get Fare Quote")
                }
            }
        } else {
            Button(
                onClick = {
                    if (activity != null && quoteResult != null) {
                        val amountInKobo = (quoteResult!!.total_fare * 100).toLong()
                        CheckoutHelper.startCardCheckout(
                            activity = activity,
                            email = userEmail,
                            amountInKobo = amountInKobo,
                            onSuccess = { transaction ->
                                coroutineScope.launch {
                                    isLoading = true
                                    val success = finalizeOrderAfterPayment(
                                        apiService = apiService,
                                        quoteId = quoteId!!,
                                        paymentReference = transaction.reference,
                                        recipientName = recipientName,
                                        recipientPhone = recipientPhone,
                                        notes = if (notes.isBlank()) null else notes
                                    )
                                    if (success) {
                                        onOrderComplete(transaction.reference)
                                    } else {
                                        errorMessage = "Payment confirmed, but order creation failed. Please contact support."
                                    }
                                    isLoading = false
                                }
                            },
                            onError = { error ->
                                errorMessage = error.localizedMessage ?: "Payment failed"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && recipientName.isNotBlank() && recipientPhone.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Pay ₦${quoteResult!!.total_fare} with Paystack")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

suspend fun finalizeOrderAfterPayment(
    apiService: ApiService,
    quoteId: String,
    paymentReference: String,
    recipientName: String,
    recipientPhone: String,
    notes: String?
): Boolean {
    return try {
        val request = CreateOrderRequest(
            quote_id = quoteId,
            payment_method = "card",
            recipient_name = recipientName,
            recipient_phone = recipientPhone,
            notes = notes
        )
        val response = apiService.createOrder(request)
        response.status == "SEARCHING" || response.status == "MATCHED"
    } catch (e: Exception) {
        false
    }
}
