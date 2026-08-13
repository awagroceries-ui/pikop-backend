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
    userRole: String,
    referralCode: String,
    onNavigateToSupport: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToRecipients: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Account") })
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
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = userEmail, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = userRole, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(32.dp))

            // Referral Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("My Referral Code", style = MaterialTheme.typography.labelSmall)
                        Text(referralCode.ifBlank { "GEN-CODE" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on Pikop! Use my code $referralCode to get NGN 300 off your first delivery. Download at: https://pikop.ng")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
                text = "Pikop v1.4.0-final",
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
