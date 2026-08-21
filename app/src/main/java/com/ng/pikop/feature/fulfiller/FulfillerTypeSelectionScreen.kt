package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun FulfillerTypeSelectionScreen(
    selectedClass: String?,
    onClassSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "How will you deliver?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Select a category. You can change this later in settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        FulfillerClassCard(
            title = "Rider",
            description = "Bikes, Scooters, or Motorcycles.",
            icon = Icons.Default.DirectionsBike,
            isSelected = selectedClass == "rider",
            onClick = { onClassSelected("rider") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FulfillerClassCard(
            title = "Driver",
            description = "Cars, Vans, or Trucks.",
            icon = Icons.Default.DirectionsCar,
            isSelected = selectedClass == "driver",
            onClick = { onClassSelected("driver") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FulfillerClassCard(
            title = "Foot Agent",
            description = "Walking or Public Transit.",
            icon = Icons.Default.Person,
            isSelected = selectedClass == "agent",
            onClick = { onClassSelected("agent") }
        )
    }
}

@Composable
fun FulfillerClassCard(
    title: String, 
    description: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp, 
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, 
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
