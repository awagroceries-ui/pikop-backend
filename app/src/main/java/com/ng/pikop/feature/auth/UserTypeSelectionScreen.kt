package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ng.pikop.R

@Composable
fun UserTypeSelectionScreen(onRoleSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(70.dp)
            )

            Text(
                text = "How do you want to use Pikop?",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoleCard(
                    title = "I want to Send",
                    description = "Request deliveries.",
                    icon = Icons.Default.ShoppingBag,
                    iconColor = com.ng.pikop.ui.theme.PikopGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CUSTOMER") }
                )
                RoleCard(
                    title = "I want to Earn",
                    description = "Join fleet & deliver.",
                    icon = Icons.Default.ElectricBike,
                    iconColor = com.ng.pikop.ui.theme.PikopGold,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FULFILLER") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                RoleCard(
                    title = "I want to Sell",
                    description = "List products & grow.",
                    icon = Icons.Default.Storefront,
                    iconColor = com.ng.pikop.ui.theme.PikopOrange,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    onClick = { onRoleSelected("MERCHANT") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoleCard(
                    title = "Fleet Partner",
                    description = "Bring your fleet.",
                    icon = Icons.Default.LocalShipping,
                    iconColor = com.ng.pikop.ui.theme.PikopGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FLEET_PARTNER") }
                )
                RoleCard(
                    title = "Business Account",
                    description = "Team billing & limits.",
                    icon = Icons.Default.Business,
                    iconColor = Color(0xFF2196F3), // Bright Dodger Blue: High contrast in Light and Dark Mode
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CORPORATE") }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = { onRoleSelected("LOGIN") }) {
                Text("Already have an account? Log In", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = iconColor.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1
            )
        }
    }
}
