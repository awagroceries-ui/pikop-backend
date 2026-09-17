package com.ng.pikop.feature.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@Composable
fun MerchantAnalyticsScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var selectedRange by remember { mutableStateOf("weekly") }
    var analyticsData by remember { mutableStateOf<MerchantAnalyticsData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchAnalytics() {
        scope.launch {
            isLoading = true
            try {
                val response = apiService.getMerchantAnalytics(selectedRange)
                analyticsData = response.data
            } catch (e: Exception) {
                android.util.Log.e("MerchantAnalytics", "Fetch failed", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedRange) {
        fetchAnalytics()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Range Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("daily", "weekly", "monthly", "annual").forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(range.replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (analyticsData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available for this range.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryStatsRow(analyticsData!!)
                }

                item {
                    Text("Sales Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TrendList(analyticsData!!.trend)
                }

                item {
                    Text("Best Selling Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    BestSellersList(analyticsData!!.best_sellers)
                }

                item {
                    Text("Peak Hours", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    PeakTimesList(analyticsData!!.peak_times)
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryStatsRow(data: MerchantAnalyticsData) {
    val totalRevenue = data.trend.sumOf { it.net_revenue }
    val totalVolume = data.trend.sumOf { it.volume }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Net Revenue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("₦${"%,.0f".format(totalRevenue)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Orders", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("$totalVolume", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Repeat Customer Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("${data.retention.repeat_customer_rate}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("${data.retention.total_unique_customers} unique customers", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun TrendList(trend: List<AnalyticsTrendItem>) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            if (trend.isEmpty()) {
                Text("No data points yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            trend.takeLast(7).forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.period.take(16), style = MaterialTheme.typography.bodySmall)
                    Text("₦${"%,.0f".format(item.net_revenue)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun BestSellersList(items: List<BestSellerItem>) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            if (items.isEmpty()) {
                Text("None yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${index + 1}", modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text("${item.units} sold", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Text("₦${"%,.0f".format(item.revenue)}", fontWeight = FontWeight.Bold)
                }
                if (index < items.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

@Composable
fun PeakTimesList(peaks: List<PeakTimeItem>) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            if (peaks.isEmpty()) {
                Text("Not enough data yet.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            peaks.forEach { peak ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${days[peak.day.toInt()]} at ${peak.hour}:00", style = MaterialTheme.typography.bodyMedium)
                    Text("${peak.volume} orders", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
