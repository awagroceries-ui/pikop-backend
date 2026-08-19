package com.ng.pikop.feature.order

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.android.gms.maps.model.LatLng
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.AutocompletePrediction
import com.ng.pikop.core.network.SavedAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchSheet(
    title: String,
    onDismiss: () -> Unit,
    onAddressSelected: (String, LatLng) -> Unit,
    onOpenMap: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = apiService.getSavedAddresses()
            savedAddresses = response.addresses
        } catch (_: Exception) {}
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with Input
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchJob?.cancel()
                            if (it.length > 2) {
                                searchJob = scope.launch {
                                    delay(300)
                                    isSearching = true
                                    try {
                                        val res = apiService.getAutocomplete(it, sessionToken)
                                        suggestions = res.predictions
                                    } catch (_: Exception) {}
                                    isSearching = false
                                }
                            } else {
                                suggestions = emptyList()
                            }
                        },
                        placeholder = { Text("Search for $title...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; suggestions = emptyList() }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // 1. Current Location (Top action)
                item {
                    ListItem(
                        headlineContent = { Text("Use my current location", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { /* Reverse geocode current loc logic */ }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }

                // 2. Map Picker (Pinned action)
                item {
                    ListItem(
                        headlineContent = { Text("Set location on map", fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.secondary) },
                        modifier = Modifier.clickable { onOpenMap() }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }

                // 3. Saved Addresses
                if (query.isEmpty()) {
                    item {
                        Text(
                            "SAVED PLACES", 
                            modifier = Modifier.padding(16.dp), 
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    items(savedAddresses) { addr ->
                        ListItem(
                            headlineContent = { Text(addr.label ?: "Saved Place", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(addr.address_text ?: "", maxLines = 1) },
                            leadingContent = { 
                                Icon(
                                    imageVector = if(addr.label == "Home") Icons.Default.Home else if(addr.label == "Work") Icons.Default.Work else Icons.Default.Star, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            modifier = Modifier.clickable {
                                onAddressSelected(addr.address_text ?: "", LatLng(addr.lat ?: 0.0, addr.lng ?: 0.0))
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }

                // 4. Live Results
                items(suggestions) { p ->
                    ListItem(
                        headlineContent = { Text(p.main_text, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(p.secondary_text, maxLines = 1) },
                        leadingContent = { Icon(Icons.Default.Place, null, tint = Color.Gray) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                try {
                                    val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                    onAddressSelected(details.formatted_address, LatLng(details.lat, details.lng))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Failed to resolve location", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
