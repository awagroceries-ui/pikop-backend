package com.ng.pikop.feature.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.Bank
import com.ng.pikop.core.network.ProfileUpdateRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var banks by remember { mutableStateOf<List<Bank>>(emptyList()) }
    var selectedBank by remember { mutableStateOf<Bank?>(null) }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    
    var role by remember { mutableStateOf("CUSTOMER") }
    var isLoading by remember { mutableStateOf(false) }
    var isVerifyingBank by remember { mutableStateOf(false) }
    var isBankListLoading by remember { mutableStateOf(true) }
    var bankExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val profile = apiService.getUserProfile()
            name = profile.full_name ?: ""
            phone = profile.phone ?: ""
            accountNumber = profile.account_number ?: ""
            accountName = profile.account_name ?: ""
            role = profile.role ?: "CUSTOMER"
            
            val bankRes = apiService.getBanks()
            banks = bankRes.data
            selectedBank = banks.find { it.name == profile.bank_name }
        } catch (e: Exception) {
            android.util.Log.e("PikopProfile", "Failed to load profile", e)
        } finally {
            isBankListLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            if (role == "FULFILLER") {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Payout Bank Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (isBankListLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    ExposedDropdownMenuBox(
                        expanded = bankExpanded,
                        onExpandedChange = { bankExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedBank?.name ?: "Select Bank",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Bank Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = bankExpanded,
                            onDismissRequest = { bankExpanded = false }
                        ) {
                            banks.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank.name) },
                                    onClick = {
                                        selectedBank = bank
                                        bankExpanded = false
                                        accountName = "" // Reset verified name
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { 
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                            accountNumber = it
                            accountName = "" // Reset verified name
                        }
                    },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                if (accountName.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Verified Name", style = MaterialTheme.typography.labelSmall)
                                Text(accountName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (accountNumber.length == 10 && selectedBank != null && accountName.isBlank()) {
                    Button(
                        onClick = {
                            scope.launch {
                                isVerifyingBank = true
                                try {
                                    val res = apiService.resolveAccount(mapOf(
                                        "account_number" to accountNumber,
                                        "bank_code" to selectedBank!!.code
                                    ))
                                    if (res.status && res.data != null) {
                                        accountName = res.data.account_name
                                    } else {
                                        Toast.makeText(context, "Could not verify account", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally { isVerifyingBank = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !isVerifyingBank
                    ) {
                        if (isVerifyingBank) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Verify Bank Account")
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            apiService.updateUserProfile(ProfileUpdateRequest(
                                full_name = name,
                                phone = phone,
                                bank_name = if (role == "FULFILLER") selectedBank?.name else null,
                                account_number = if (role == "FULFILLER") accountNumber else null,
                                bank_code = if (role == "FULFILLER") selectedBank?.code else null,
                                account_name = if (role == "FULFILLER") accountName else null
                            ))
                            
                            // Update local store so UI refreshes immediately
                            tokenManager.saveTokens(
                                accessToken = tokenManager.accessToken.first() ?: "",
                                refreshToken = tokenManager.refreshToken.first() ?: "",
                                email = tokenManager.userEmail.first() ?: "",
                                role = tokenManager.userRole.first() ?: "CUSTOMER",
                                name = name,
                                phone = phone,
                                referralCode = tokenManager.referralCode.first()
                            )
                            
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && name.isNotBlank() && phone.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Save Changes")
            }
        }
    }
}
