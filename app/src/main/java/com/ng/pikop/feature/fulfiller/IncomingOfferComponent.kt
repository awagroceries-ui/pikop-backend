package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ng.pikop.core.network.OfferResponse

@Composable
fun IncomingOfferComponent(
    offer: OfferResponse,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Item Photo Preview
            if (offer.item_photo_url != null) {
                AsyncImage(
                    model = "https://api.awa.name.ng${offer.item_photo_url}",
                    contentDescription = "Item Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "New Delivery Offer", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "₦${offer.total_fare ?: 0.0}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Pickup Region", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(text = offer.pickup_address ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "Dropoff Info", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(text = offer.delivery_address ?: "N/A", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                        Text("Decline")
                    }
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Text("Accept Mission")
                    }
                }
            }
        }
    }
}
