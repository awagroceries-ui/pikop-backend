package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.Bank
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.SetupMerchantRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantBusinessSetupScreen(
    onSetupSuccess: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var businessName by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var cacNumber by remember { mutableStateOf("") }
    var nafdacNumber by remember { mutableStateOf("") }
    
    // Paystack Bank State
    var banksList by remember { mutableStateOf<List<Bank>>(emptyList()) }
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var bankSearchText by remember { mutableStateOf("") }
    var expandedBankDropdown by remember { mutableStateOf(false) }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf<String?>(null) }
    var isResolvingAccount by remember { mutableStateOf(false) }
    
    var acceptsCod by remember { mutableStateOf(true) }
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("Food", "Groceries", "Shop")
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    // Fetch Paystack Bank List on Init
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getBanks()
            if (response.status && response.data.isNotEmpty()) {
                banksList = response.data
            }
        } catch (_: Exception) {}
    }

    // Auto-resolve Paystack Account Name when 10 digits entered
    LaunchedEffect(accountNumber, selectedBank) {
        if (accountNumber.length == 10 && selectedBank != null) {
            isResolvingAccount = true
            accountName = null
            try {
                val res = apiService.resolveAccount(
                    mapOf(
                        "account_number" to accountNumber,
                        "bank_code" to selectedBank!!.code
                    )
                )
                if (res.status && !res.data?.account_name.isNullOrBlank()) {
                    accountName = res.data?.account_name
                }
            } catch (_: Exception) {
                accountName = null
            } finally {
                isResolvingAccount = false
            }
        } else {
            accountName = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Business Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Step 2: Tell us about your business.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = it }
            ) {
                OutlinedTextField(
                    value = businessCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Business Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                businessCategory = selectionOption
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = businessAddress,
                onValueChange = { businessAddress = it },
                label = { Text("Business Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cacNumber,
                onValueChange = { cacNumber = it },
                label = { Text("CAC Registration Number (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // NAFDAC conditional
            if (businessCategory == "Food" || businessCategory == "Groceries") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nafdacNumber,
                    onValueChange = { nafdacNumber = it },
                    label = { Text("NAFDAC Number (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Payout Details (Paystack Verified)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Paystack Bank Dropdown
            val filteredBanks = banksList.filter { 
                it.name.contains(bankSearchText, ignoreCase = true) 
            }

            ExposedDropdownMenuBox(
                expanded = expandedBankDropdown,
                onExpandedChange = { expandedBankDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedBank?.name ?: bankSearchText,
                    onValueChange = { 
                        bankSearchText = it
                        selectedBank = null
                        expandedBankDropdown = true 
                    },
                    label = { Text("Select Bank") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBankDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (filteredBanks.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedBankDropdown,
                        onDismissRequest = { expandedBankDropdown = false }
                    ) {
                        filteredBanks.take(20).forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.name) },
                                onClick = {
                                    selectedBank = bank
                                    bankSearchText = bank.name
                                    expandedBankDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) accountNumber = it },
                label = { Text("10-Digit Account Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (isResolvingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )

            // Resolved Account Name Card
            if (!accountName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ Verified: $accountName",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Payment Preference", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Accept COD Orders", fontWeight = FontWeight.Bold)
                            Text("Customers can pay on delivery. Funds are held in escrow and released to you after delivery.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(checked = acceptsCod, onCheckedChange = { acceptsCod = it })
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val request = SetupMerchantRequest(
                                business_name = businessName,
                                category = businessCategory,
                                address = businessAddress,
                                cac_number = cacNumber.ifBlank { null },
                                nafdac_number = nafdacNumber.ifBlank { null },
                                bank_name = selectedBank?.name ?: bankSearchText,
                                account_number = accountNumber,
                                accepts_cod = acceptsCod,
                                bank_code = selectedBank?.code,
                                account_name = accountName
                            )
                            val response = apiService.setupMerchantProfile(request)
                            if (response.success == true) {
                                onSetupSuccess()
                            } else {
                                errorMessage = response.message
                            }
                        } catch (e: Exception) {
                            errorMessage = ErrorUtils.parseError(e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && businessName.isNotBlank() && businessCategory.isNotBlank() && businessAddress.isNotBlank() && (selectedBank != null || bankSearchText.isNotBlank()) && accountNumber.length == 10
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit for Verification")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
