package com.ng.pikop.feature.wallet

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.WithdrawalRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var amount by remember { mutableStateOf("") }
    var walletBalance by remember { mutableStateOf(0.0) }
    var bankName by remember { mutableStateOf("Loading...") }
    var accountNumber by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            val wallet = apiService.getWalletInfo()
            walletBalance = wallet.balance ?: 0.0
            
            val profile = apiService.getFulfillerProfile()
            bankName = profile.bank_name ?: "No Bank Linked"
            accountNumber = profile.account_number ?: ""
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading wallet info", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Withdrawal") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Available for Withdrawal", style = MaterialTheme.typography.labelSmall)
                        Text("₦${"%,.2f".format(walletBalance)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to Withdraw (₦)") },
                    placeholder = { Text("Min. ₦1,000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₦") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bank Details Confirmation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Recipient Bank Account", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(bankName, fontWeight = FontWeight.Bold)
                            Text(accountNumber.ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (accountNumber.isBlank()) {
                    Text(
                        "Please update your bank details in profile settings to enable withdrawals.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val numericAmount = amount.toDoubleOrNull() ?: 0.0
                val canSubmit = !isSubmitting && numericAmount >= 1000 && numericAmount <= walletBalance && accountNumber.isNotBlank()

                Button(
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            try {
                                val response = apiService.requestWithdrawal(WithdrawalRequest(
                                    amount = numericAmount,
                                    type = if (numericAmount <= 5000) "INSTANT" else "STANDARD"
                                ))
                                Toast.makeText(context, response.message ?: "Request Submitted", Toast.LENGTH_LONG).show()
                                onSuccess()
                            } catch (e: Exception) {
                                val error = ErrorUtils.parseError(e)
                                Toast.makeText(context, "Withdrawal Failed: $error", Toast.LENGTH_LONG).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSubmit
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Request Withdrawal")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Standard withdrawals take 1-3 business days. Instant payouts (under ₦5,000) are processed immediately.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
