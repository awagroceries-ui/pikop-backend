package com.ng.pikop.feature.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
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
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("CUSTOMER") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val profile = apiService.getUserProfile()
            name = profile.full_name ?: ""
            phone = profile.phone ?: ""
            bankName = profile.bank_name ?: ""
            accountNumber = profile.account_number ?: ""
            role = profile.kyc_status?.let { "FULFILLER" } ?: "CUSTOMER" // Basic check
        } catch (e: Exception) {
            android.util.Log.e("PikopProfile", "Failed to load profile", e)
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
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { /* Usually selected via dropdown in KYC, keeping read-only here for now */ },
                    label = { Text("Bank Name") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp)) }
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { /* Read-only here to prevent accidental changes to verified accounts */ },
                    label = { Text("Account Number") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp)) }
                )
                Text("To update bank details, please contact support or use the Account Activation flow.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            apiService.updateUserProfile(ProfileUpdateRequest(
                                full_name = name,
                                phone = phone
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
