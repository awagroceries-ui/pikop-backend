package com.ng.pikop.feature.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.WalletTransaction
import com.ng.pikop.core.network.WithdrawalRequest
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(onBack: () -> Unit, isFulfiller: Boolean = false) {
    var balance by remember { mutableStateOf(0.0) }
    var transactions by remember { mutableStateOf<List<WalletTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    val fetchWallet = suspend {
        isLoading = true
        try {
            val response = apiService.getWalletInfo()
            balance = response.balance ?: 0.0
            transactions = response.transactions ?: emptyList()
        } catch (e: Exception) {}
        isLoading = false
    }

    LaunchedEffect(Unit) {
        fetchWallet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && transactions.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painter = painterResource(id = R.drawable.pikop_logo), contentDescription = null, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Current Balance", style = MaterialTheme.typography.labelMedium)
                            Text("₦${"%,.2f".format(balance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            if (isFulfiller && balance > 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showWithdrawDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Request Payout")
                                }
                            }
                        }
                    }
                    Text(text = "Recent Activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No transactions yet.", color = Color.Gray) }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(transactions) { tx -> TransactionItem(tx) }
                        }
                    }
                }
            }
        }
    }
    if (showWithdrawDialog) {
        WithdrawalDialog(onDismiss = { showWithdrawDialog = false }, onConfirm = { amount, type -> scope.launch { try { apiService.requestWithdrawal(WithdrawalRequest(amount, type)); showWithdrawDialog = false; fetchWallet() } catch (e: Exception) {} } }, maxAmount = balance)
    }
}

@Composable
fun TransactionItem(tx: WalletTransaction) {
    val isCredit = tx.entry_type == "CREDIT"
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (isCredit) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade, contentDescription = null, tint = if (isCredit) Color(0xFF388E3C) else Color(0xFFC62828))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = (tx.purpose ?: "Transaction").replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.bodyMedium)
                Text(text = (tx.created_at ?: "").take(10), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Text(text = "${if (isCredit) "+" else "-"}₦${tx.amount ?: 0.0}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isCredit) Color(0xFF388E3C) else Color(0xFFC62828))
        }
    }
}

@Composable
fun WithdrawalDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit, maxAmount: Double) {
    var amount by remember { mutableStateOf(maxAmount.toString()) }
    var type by remember { mutableStateOf("INSTANT") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Request Payout") }, text = { Column { Text("Enter amount to withdraw to your linked bank account.", style = MaterialTheme.typography.bodySmall); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (NGN)") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = type == "INSTANT", onClick = { type = "INSTANT" }); Text("Instant (Fee applies)"); Spacer(modifier = Modifier.width(16.dp)); RadioButton(selected = type == "STANDARD", onClick = { type = "STANDARD" }); Text("Standard") } } }, confirmButton = { Button(onClick = { val amt = amount.toDoubleOrNull() ?: 0.0; if (amt > 0 && amt <= maxAmount) onConfirm(amt, type) }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
