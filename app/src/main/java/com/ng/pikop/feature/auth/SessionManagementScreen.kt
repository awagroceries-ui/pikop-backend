package com.ng.pikop.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.UserSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }

    var sessions by remember { mutableStateOf<List<UserSession>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val fetchSessions = {
        scope.launch {
            isLoading = true
            try {
                sessions = apiService.getActiveSessions()
            } catch (e: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchSessions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Sessions") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                "These devices are currently logged into your account. You can log out of any device remotely.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading && sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(sessions) { session ->
                        SessionItem(session, onLogout = {
                            scope.launch {
                                try {
                                    apiService.revokeSession(session.id!!)
                                    fetchSessions()
                                } catch (e: Exception) {}
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: UserSession, onLogout: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.device_name ?: "Unknown Device", fontWeight = FontWeight.Bold)
                Text("IP: ${session.ip_address}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("Last active: ${session.last_active}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, null, tint = Color.Red)
            }
        }
    }
}
