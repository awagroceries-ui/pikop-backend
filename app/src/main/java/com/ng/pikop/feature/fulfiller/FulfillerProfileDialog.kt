package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ng.pikop.R
import com.ng.pikop.core.network.FulfillerPublicProfile

@Composable
fun FulfillerProfileDialog(
    profile: FulfillerPublicProfile,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Photo & Tier
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (profile.profile_photo_url != null) {
                        AsyncImage(
                            model = "https://api.pikop.com.ng${profile.profile_photo_url}",
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    val tierColor = when(profile.tier?.lowercase()) {
                        "elite" -> Color(0xFF6200EE)
                        "super" -> Color(0xFFFFD700)
                        "standard" -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32) // Basic / Bronze
                    }
                    
                    Surface(
                        color = tierColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.offset(x = 4.dp, y = 4.dp)
                    ) {
                        Text(
                            text = profile.tier ?: "Basic",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profile.full_name ?: "Agent",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                val roleLabel = when(profile.primary_class?.lowercase()) {
                    "rider" -> "Professional Rider"
                    "driver" -> "Professional Driver"
                    else -> "Field Agent"
                }
                Text(text = roleLabel, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Rating", "${profile.rating_avg ?: 5.0}", Icons.Default.Star, Color(0xFFFF9F0A))
                    StatItem("Missions", "${profile.rating_count ?: 0}", Icons.Default.History, MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Vehicle Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val vehicleIcon = when(profile.primary_class?.lowercase()) {
                            "rider" -> Icons.Default.ElectricBike
                            "driver" -> Icons.Default.DirectionsCar
                            else -> Icons.Default.DirectionsWalk
                        }
                        Icon(vehicleIcon, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = profile.make ?: "Registered Vehicle", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = profile.vehicle_registration_number ?: "Verified Member",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close Profile")
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}
