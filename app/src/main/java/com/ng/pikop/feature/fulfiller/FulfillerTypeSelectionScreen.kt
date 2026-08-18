package com.ng.pikop.feature.fulfiller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ProfileUpdateRequest
import kotlinx.coroutines.launch

@Composable
fun FulfillerTypeSelectionScreen(onClassSelected: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
            "Select the category that matches your transportation mode.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        FulfillerClassCard(
            title = "Rider",
            description = "Bikes, Scooters, or Motorcycles.",
            icon = Icons.Default.DirectionsBike,
            onClick = {
                selectClass(scope, apiService, "rider", onClassSelected) { isLoading = it }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FulfillerClassCard(
            title = "Driver",
            description = "Cars, Vans, or Trucks.",
            icon = Icons.Default.DirectionsCar,
            onClick = {
                selectClass(scope, apiService, "driver", onClassSelected) { isLoading = it }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        FulfillerClassCard(
            title = "Foot Agent",
            description = "Walking or Public Transit.",
            icon = Icons.Default.Person,
            onClick = {
                selectClass(scope, apiService, "agent", onClassSelected) { isLoading = it }
            }
        )
        
        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun selectClass(
    scope: kotlinx.coroutines.CoroutineScope,
    api: ApiService,
    className: String,
    onSuccess: () -> Unit,
    setLoading: (Boolean) -> Unit
) {
    scope.launch {
        setLoading(true)
        try {
            api.updateFulfillerProfile(ProfileUpdateRequest(primary_class = className))
            onSuccess()
        } catch (_: Exception) {}
        setLoading(false)
    }
}

@Composable
fun FulfillerClassCard(title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}
