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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "How do you want to use Pikop?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoleCard(
                    title = "I want to Send",
                    description = "Request deliveries & track items.",
                    icon = Icons.Default.ShoppingBag,
                    iconColor = com.ng.pikop.ui.theme.PikopGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CUSTOMER") }
                )
                RoleCard(
                    title = "I want to Earn",
                    description = "Join the fleet & deliver items.",
                    icon = Icons.Default.ElectricBike,
                    iconColor = com.ng.pikop.ui.theme.PikopGold,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FULFILLER") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoleCard(
                    title = "I want to Sell",
                    description = "List products & grow business.",
                    icon = Icons.Default.Storefront,
                    iconColor = com.ng.pikop.ui.theme.PikopOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("MERCHANT") }
                )
                RoleCard(
                    title = "Fleet Partner",
                    description = "Bring your fleet onto Pikop.",
                    icon = Icons.Default.LocalShipping,
                    iconColor = com.ng.pikop.ui.theme.PikopGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("FLEET_PARTNER") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoleCard(
                    title = "Business Account",
                    description = "Centralized billing for teams.",
                    icon = Icons.Default.Business,
                    iconColor = com.ng.pikop.ui.theme.PikopNearBlack,
                    modifier = Modifier.weight(1f),
                    onClick = { onRoleSelected("CORPORATE") }
                )
                // Spacer to keep layout balanced
                Spacer(modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            TextButton(onClick = { onRoleSelected("LOGIN") }) {
                Text("Already have an account? Log In", color = MaterialTheme.colorScheme.primary)
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
            .height(180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = iconColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = iconColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
