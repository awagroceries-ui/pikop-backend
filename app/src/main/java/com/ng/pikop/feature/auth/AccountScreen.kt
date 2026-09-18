package com.ng.pikop.feature.auth

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
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
import com.ng.pikop.core.network.ErrorUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    userEmail: String,
    userName: String,
    userRole: String,
    referralCode: String,
    kycStatus: String? = null,
    onNavigateToSupport: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToCorporate: () -> Unit,
    onNavigateToGrowth: () -> Unit,
    onNavigateToMerchant: () -> Unit,
    onNavigateToMerchantRegistration: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isActionLoading by remember { mutableStateOf(false) }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { old, new ->
                scope.launch {
                    isActionLoading = true
                    try {
                        apiService.changePassword(mapOf("old_password" to old, "new_password" to new))
                        Toast.makeText(context, "Password Changed", Toast.LENGTH_SHORT).show()
                        showChangePasswordDialog = false
                    } catch (e: Exception) {
                        Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_LONG).show()
                    } finally { isActionLoading = false }
                }
            },
            isLoading = isActionLoading
        )
    }

    if (showDeleteAccountDialog) {
        var deletionStep by remember { mutableIntStateOf(1) } // 1: Warning, 2: Password, 3: Confirm
        var passwordInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false; deletionStep = 1 },
            title = { 
                Text(
                    text = when(deletionStep) {
                        1 -> "Delete Account?"
                        2 -> "Verify Identity"
                        else -> "Final Confirmation"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (deletionStep) {
                        1 -> {
                            Text("This action is permanent and irreversible. You will lose access to your wallet, rewards, and order history.")
                            Text("All personal data will be removed or anonymized in compliance with NDPA regulations.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        2 -> {
                            Text("Please enter your current password to continue.")
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Password") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        3 -> {
                            Text("Are you absolutely sure? This cannot be undone.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            when (deletionStep) {
                                1 -> deletionStep = 2
                                2 -> {
                                    isActionLoading = true
                                    try {
                                        val confirmRes = apiService.confirmPassword(mapOf("password" to passwordInput))
                                        if (confirmRes.success == true) {
                                            deletionStep = 3
                                        } else {
                                            Toast.makeText(context, confirmRes.message ?: "Incorrect password", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_LONG).show()
                                    } finally { isActionLoading = false }
                                }
                                3 -> {
                                    isActionLoading = true
                                    try {
                                        val deleteRes = apiService.deleteAccount()
                                        if (deleteRes.success == true) {
                                            Toast.makeText(context, "Account Deleted", Toast.LENGTH_LONG).show()
                                            onLogout()
                                        } else {
                                            // Handle block conditions (active missions, etc)
                                            Toast.makeText(context, deleteRes.message, Toast.LENGTH_LONG).show()
                                            showDeleteAccountDialog = false
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, ErrorUtils.parseError(e), Toast.LENGTH_LONG).show()
                                    } finally { isActionLoading = false }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (deletionStep == 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isActionLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text(
                        when(deletionStep) {
                            1 -> "Continue"
                            2 -> "Verify"
                            else -> "Delete Forever"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false; deletionStep = 1 }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = com.ng.pikop.ui.theme.PikopNearBlack
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName.ifBlank { "User" }, 
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (kycStatus == "VERIFIED") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.pikop_badge),
                        contentDescription = "Verified",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = userEmail, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = userRole, 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Referral Card (Gold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My Referral Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary)
                        Text(referralCode.ifBlank { "GEN-CODE" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondary)
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on Pikop! Use my code $referralCode to get NGN 250 off your first delivery. Download at: https://pikop.ng")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }

            // Options List
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountOption(
                    label = "Edit Profile",
                    icon = Icons.Default.Edit,
                    onClick = onNavigateToProfile
                )

                AccountOption(
                    label = "Rewards & Referrals",
                    icon = Icons.Default.Stars,
                    onClick = onNavigateToGrowth
                )

                // Role-Aware Merchant Access (Optimization Milestone 31)
                val isMerchant = userRole == "MERCHANT"
                AccountOption(
                    label = if (isMerchant) "Manage My Shop" else "Add Merchant Profile",
                    icon = if (isMerchant) Icons.Default.Storefront else Icons.Default.AddBusiness,
                    onClick = {
                        scope.launch {
                            try {
                                val profileRes = apiService.getMerchantProfile()
                                if (profileRes.data != null) {
                                    onNavigateToMerchant()
                                } else {
                                    // Missing profile record (Legacy Merchant Gap)
                                    onNavigateToMerchantRegistration()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not verify merchant status", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                if (userRole == "CUSTOMER") {
                    AccountOption(
                        label = "Saved Addresses",
                        icon = Icons.Default.LocationOn,
                        onClick = onNavigateToAddresses
                    )
                    AccountOption(
                        label = "Saved Recipients",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToRecipients
                    )
                    AccountOption(
                        label = "Corporate Accounts",
                        icon = Icons.Default.Business,
                        onClick = onNavigateToCorporate
                    )
                }

                AccountOption(
                    label = "Notifications",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotifications
                )

                AccountOption(
                    label = "Support & Help Center",
                    icon = Icons.AutoMirrored.Filled.HelpCenter,
                    onClick = onNavigateToSupport
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Legal", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                AccountOption(
                    label = "Terms & Conditions",
                    icon = Icons.Default.Description,
                    onClick = { onNavigateToTerms() }
                )

                AccountOption(
                    label = "Privacy Policy",
                    icon = Icons.Default.Shield,
                    onClick = { onNavigateToPrivacy() }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Account Management", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

                AccountOption(
                    label = "Manage Active Sessions",
                    icon = Icons.Default.Devices,
                    onClick = onNavigateToSessions
                )

                AccountOption(
                    label = "Change Password",
                    icon = Icons.Default.Lock,
                    onClick = { 
                        android.util.Log.d("AccountScreen", "Change Password Clicked")
                        showChangePasswordDialog = true 
                    }
                )

                AccountOption(
                    label = "Delete Account",
                    icon = Icons.Default.DeleteForever,
                    onClick = { 
                        android.util.Log.d("AccountScreen", "Delete Account Clicked")
                        showDeleteAccountDialog = true 
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
            
            Text(
                text = "Pikop v2.1.6-stable",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
fun AccountOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}
