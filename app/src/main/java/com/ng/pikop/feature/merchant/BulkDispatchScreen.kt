package com.ng.pikop.feature.merchant

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDispatchScreen(
    navController: NavController,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var batchName by remember { mutableStateOf("") }
    val missions = remember { mutableStateListOf<BulkOrderMission>() }
    
    var merchantProfile by remember { mutableStateOf<MerchantProfile?>(null) }
    var merchantLat by remember { mutableDoubleStateOf(0.0) }
    var merchantLng by remember { mutableDoubleStateOf(0.0) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch Merchant Profile (For fixed pickup address)
    LaunchedEffect(Unit) {
        try {
            val profileRes = apiService.getMerchantProfile()
            merchantProfile = profileRes.data
        } catch (e: Exception) {
            android.util.Log.e("BulkDispatch", "Profile fetch failed", e)
        } finally {
            isLoading = false
        }
    }

    // Capture Address from Map Search (Shared with row adding)
    val selectedAddress by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<String>("bulk_row_address", "")
        ?.collectAsState() ?: remember { mutableStateOf("") }
    
    val selectedLat by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<Double>("bulk_row_lat", 0.0)
        ?.collectAsState() ?: remember { mutableDoubleStateOf(0.0) }
        
    val selectedLng by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<Double>("bulk_row_lng", 0.0)
        ?.collectAsState() ?: remember { mutableDoubleStateOf(0.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Dispatch", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Entry")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it },
                    label = { Text("Batch Name (e.g. Monday Deliveries)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
                
                if (missions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inventory2, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Text("No missions in this batch yet.", color = Color.Gray)
                            Text("Click + to add your first entry.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(missions) { index, mission ->
                            BulkMissionItem(mission) { missions.removeAt(index) }
                        }
                    }
                }
                
                val totalCost = missions.sumOf { it.total_fare }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Batch Cost", style = MaterialTheme.typography.labelSmall)
                            Text("₦${"%,.2f".format(totalCost)}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isSubmitting = true
                                    try {
                                        apiService.createBulkOrders(BulkOrderRequest(batchName.ifBlank { null }, missions))
                                        Toast.makeText(context, "Batch Dispatched Successfully!", Toast.LENGTH_LONG).show()
                                        onSuccess()
                                    } catch (e: Exception) {
                                        val err = ErrorUtils.parseError(e)
                                        Toast.makeText(context, "Dispatch Failed: $err", Toast.LENGTH_LONG).show()
                                    } finally { isSubmitting = false }
                                }
                            },
                            enabled = missions.isNotEmpty() && !isSubmitting,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Dispatch All (${missions.size})")
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddBulkEntryDialog(
            initialAddress = selectedAddress,
            onDismiss = { showAddDialog = false },
            onSelectAddress = { navController.navigate("map_address_search/Recipient Location/bulk_row") },
            onConfirm = { name, phone, desc, addr, lat, lng ->
                missions.add(BulkOrderMission(
                    recipient_name = name,
                    recipient_phone = phone,
                    item_description = desc,
                    pickup_address = "Merchant Location", 
                    delivery_address = addr,
                    pickup_lat = merchantLat,
                    pickup_lng = merchantLng,
                    delivery_lat = lat,
                    delivery_lng = lng,
                    total_fare = 1500.0 
                ))
                showAddDialog = false
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("bulk_row_address")
            }
        )
    }
}

@Composable
fun BulkMissionItem(mission: BulkOrderMission, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(mission.recipient_name, fontWeight = FontWeight.Bold)
                Text(mission.delivery_address, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = Color.Gray)
                Text("Item: ${mission.item_description}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, null, tint = Color.Red) }
        }
    }
}

@Composable
fun AddBulkEntryDialog(
    initialAddress: String,
    onDismiss: () -> Unit,
    onSelectAddress: () -> Unit,
    onConfirm: (String, String, String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recipient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Item Description") }, modifier = Modifier.fillMaxWidth())
                
                OutlinedCard(onClick = onSelectAddress, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (initialAddress.isBlank()) "Select Address" else initialAddress, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, desc, initialAddress, 0.0, 0.0) }, 
                enabled = name.isNotBlank() && phone.isNotBlank() && initialAddress.isNotBlank()
            ) { Text("Add to Batch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
