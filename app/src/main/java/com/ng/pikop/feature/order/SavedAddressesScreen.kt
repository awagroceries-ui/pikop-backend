package com.ng.pikop.feature.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.AddressRequest
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.SavedAddress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    navController: NavHostController,
    onBack: () -> Unit
) {
    var addresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf<LatLng?>(null) }
    var tempAddressText by remember { mutableStateOf("") }

    // Observers for result from Map search
    val newAddr by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("new_address", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val newLat by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("new_lat", null)?.collectAsState() ?: remember { mutableStateOf(null) }
    val newLng by navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<Double?>("new_lng", null)?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(newAddr, newLat, newLng) {
        if (newAddr != null && newLat != null && newLng != null) {
            tempAddressText = newAddr!!
            showLabelDialog = LatLng(newLat!!, newLng!!)
            navController.currentBackStackEntry?.savedStateHandle?.set("new_address", null)
        }
    }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    val fetchAddresses: suspend () -> Unit = {
        isLoading = true
        try {
            val response = apiService.getSavedAddresses()
            addresses = response.addresses
        } catch (e: Exception) {}
        isLoading = false
    }

    LaunchedEffect(Unit) {
        fetchAddresses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("map_address_search/Add Address/new") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Address")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (addresses.isEmpty()) {
                Text("No saved addresses yet.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(addresses) { addr ->
                        AddressItem(
                            address = addr,
                            onDelete = {
                                scope.launch {
                                    try {
                                        apiService.deleteAddress(addr.id ?: 0)
                                        fetchAddresses()
                                    } catch (e: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLabelDialog != null) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLabelDialog = null },
            title = { Text("Label this location") },
            text = {
                TextField(value = label, onValueChange = { label = it }, placeholder = { Text("e.g. Home, Office") })
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        try {
                            apiService.saveAddress(SavedAddress(
                                label = label,
                                address_text = tempAddressText,
                                lat = showLabelDialog!!.latitude,
                                lng = showLabelDialog!!.longitude
                            ))
                            showLabelDialog = null
                            fetchAddresses()
                        } catch (e: Exception) {}
                    }
                }, enabled = label.isNotBlank()) {
                    Text("Save")
                }
            }
        )
    }
}

@Composable
fun AddressItem(address: SavedAddress, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if ((address.label ?: "").lowercase() == "home") Icons.Default.Home else Icons.Default.Work,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = address.label ?: "Location", style = MaterialTheme.typography.titleMedium)
                }
                Text(text = address.address_text ?: "N/A", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
