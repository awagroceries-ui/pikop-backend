package com.ng.pikop.feature.fleetpartner

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetPartnerDashboardScreen(
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var dashboardData by remember { mutableStateOf<FleetDashboardData?>(null) }
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchData() {
        scope.launch {
            isLoading = true
            try {
                val res = apiService.getFleetDashboard()
                dashboardData = res.data
                
                val inviteRes = apiService.getFleetInviteCode()
                inviteCode = inviteRes["invite_code"]
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load dashboard: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fleet Partner Console", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { fetchData() }) { Icon(Icons.Default.Refresh, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (dashboardData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Account Pending Review")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header Stats
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Fleet Overview (30 Days)", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Active Missions", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("${dashboardData?.stats?.total_active_missions}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Volume", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    Text("₦${"%,.0f".format(dashboardData?.stats?.total_fleet_volume)}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    // Invite Section
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Onboard Drivers", fontWeight = FontWeight.Bold)
                                Text("Share this code with your riders during signup.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Surface(color = MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small) {
                                Text(inviteCode ?: "---", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                        }
                    }
                }

                item {
                    Text("Fleet Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (dashboardData?.fulfillers?.isEmpty() == true) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No linked drivers yet.", color = Color.Gray)
                        }
                    }
                } else {
                    items(dashboardData?.fulfillers ?: emptyList()) { f ->
                        FulfillerRow(f)
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun FulfillerRow(f: FulfillerProfileResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(f.full_name ?: "Unknown", fontWeight = FontWeight.Bold)
                Text("${f.primary_class?.uppercase()} • ${f.online_status}", style = MaterialTheme.typography.bodySmall, color = if(f.online_status == "ONLINE") Color(0xFF008751) else Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("⭐ ${"%.1f".format(f.rating_avg ?: 5.0)}", fontWeight = FontWeight.Bold)
                Text("${f.kyc_status}", style = MaterialTheme.typography.labelSmall, color = if(f.kyc_status == "VERIFIED") Color(0xFF008751) else Color.Red)
            }
        }
    }
}
