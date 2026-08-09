package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R

@Composable
fun NavigationDrawerContent(
    userEmail: String,
    userRole: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        drawerContentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Header
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Pikop Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = userEmail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Menu Items
            NavigationItem(
                label = "Dashboard",
                icon = Icons.Default.Dashboard,
                onClick = { onNavigate("home") }
            )
            
            NavigationItem(
                label = "My Wallet",
                icon = Icons.Default.AccountBalanceWallet,
                onClick = { onNavigate("wallet") }
            )

            if (userRole == "CUSTOMER") {
                NavigationItem(
                    label = "Saved Addresses",
                    icon = Icons.Default.LocationOn,
                    onClick = { onNavigate("addresses") }
                )
            }

            NavigationItem(
                label = "Support & Help",
                icon = Icons.AutoMirrored.Filled.HelpCenter,
                onClick = { onNavigate("about") }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Logout
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NavigationItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
