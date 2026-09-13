package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ng.pikop.R

@Composable
fun FulfillerCategorySelectionScreen(onCategorySelected: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Choose your Mobility Category",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "This determines the types of delivery missions you will receive.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoleCard(
                    title = "Foot Agent /\nCyclist",
                    description = "Short distance,\nsmall parcels.",
                    icon = Icons.Default.DirectionsWalk,
                    modifier = Modifier.weight(1f),
                    onClick = { onCategorySelected("FOOT_AGENT") }
                )
                RoleCard(
                    title = "Rider",
                    description = "Motorcycle for\nmedium parcels.",
                    icon = Icons.Default.ElectricBike,
                    modifier = Modifier.weight(1f),
                    onClick = { onCategorySelected("RIDER") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                RoleCard(
                    title = "Driver",
                    description = "Car or Van for\nlarge items.",
                    icon = Icons.Default.LocalShipping,
                    modifier = Modifier.weight(0.5f),
                    onClick = { onCategorySelected("DRIVER") }
                )
            }
        }
    }
}
