package com.ng.pikop.feature.growth

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthRewardsScreen(
    onCelebration: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<GrowthStats?>(null) }
    var referralHistory by remember { mutableStateOf<List<ReferralItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRedeeming by remember { mutableStateOf(false) }
    var showRedeemDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            try {
                val response = apiService.getGrowthStats()
                stats = response.data
                
                val historyRes = apiService.getReferralHistory()
                referralHistory = historyRes.data
            } catch (e: Exception) {
                android.util.Log.e("GrowthUI", "Failed to load stats", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards & Referrals") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Loyalty Points Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Stars, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loyalty Points", style = MaterialTheme.typography.labelMedium)
                        Text("${stats?.total_points ?: 0}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("1 pt per ₦100 spent", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        if ((stats?.total_points ?: 0) >= 500) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showRedeemDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("REDEEM POINTS")
                            }
                        }
                    }
                }

                // Referral Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Orders Completed",
                        value = "${stats?.total_orders_completed ?: 0}",
                        icon = Icons.Default.LocalShipping
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Referrals",
                        value = "${stats?.referral_count ?: 0}",
                        icon = Icons.Default.Group
                    )
                }

                // Share Referral Code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Earn ₦250 for each friend!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("They also get ₦250 off their first delivery.", style = MaterialTheme.typography.bodySmall)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = stats?.referral_code ?: "---", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = {
                                    val code = stats?.referral_code ?: ""
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Use my code $code to get ₦250 off your first delivery on Pikop! Download at: https://pikop.ng")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Referral Code"))
                                }) {
                                    Icon(Icons.Default.Share, null)
                                }
                            }
                        }
                    }
                }

                if (referralHistory.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("My Referred Friends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        referralHistory.forEach { ref ->
                            ReferralRow(ref)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showRedeemDialog) {
        AlertDialog(
            onDismissRequest = { showRedeemDialog = false },
            title = { Text("Redeem Points") },
            text = { Text("You are about to redeem ${stats?.total_points} points for ₦${stats?.total_points} credit in your wallet. This action is irreversible. Proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isRedeeming = true
                            try {
                                apiService.redeemPoints(RedeemPointsRequest(stats?.total_points ?: 0))
                                refreshData()
                                onCelebration()
                                android.widget.Toast.makeText(context, "Points Redeemed!", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Redemption failed", android.widget.Toast.LENGTH_SHORT).show()
                            } finally {
                                isRedeeming = false
                                showRedeemDialog = false
                            }
                        }
                    },
                    enabled = !isRedeeming
                ) {
                    if (isRedeeming) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Confirm Redemption")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRedeemDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ReferralRow(ref: ReferralItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(ref.full_name, fontWeight = FontWeight.Bold)
                Text("Joined: ${ref.created_at.take(10)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Badge(
                containerColor = if (ref.status == "completed") Color(0xFF008751) else Color.Gray
            ) {
                Text(
                    text = if (ref.status == "completed") "COMPLETED" else "JOINED",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
