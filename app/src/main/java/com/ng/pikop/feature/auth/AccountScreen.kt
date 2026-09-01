package com.ng.pikop.feature.auth

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R

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
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                            putExtra(Intent.EXTRA_TEXT, "Join me on Pikop! Use my code $referralCode to get NGN 300 off your first delivery. Download at: https://pikop.ng")
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

                AccountOption(
                    label = "Manage Active Sessions",
                    icon = Icons.Default.Devices,
                    onClick = onNavigateToSessions
                )

                AccountOption(
                    label = "Change Password",
                    icon = Icons.Default.Lock,
                    onClick = { /* Navigate to change password */ }
                )

                AccountOption(
                    label = "Delete Account",
                    icon = Icons.Default.DeleteForever,
                    onClick = { /* Navigate to delete request */ }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
