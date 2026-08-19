package com.ng.pikop.feature.order

import android.widget.Toast
import androidx.compose.foundation.clickable
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

@Composable
fun AddressAutocompleteField(
    label: String,
    value: String,
    onValueChange: (String, LatLng?) -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf(value) }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(value) {
        if (value != query) query = value
    }

    // Load saved addresses
    LaunchedEffect(Unit) {
        try {
            val response = apiService.getSavedAddresses()
            savedAddresses = response.addresses
        } catch (_: Exception) {}
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                if (it.isEmpty()) onValueChange("", null)
                
                searchJob?.cancel()
                if (it.length > 2) {
                    searchJob = scope.launch {
                        delay(350)
                        isSearching = true
                        try {
                            val response = apiService.getAutocomplete(it, sessionToken)
                            suggestions = response.predictions
                        } catch (_: Exception) {
                            suggestions = emptyList()
                        }
                        isSearching = false
                    }
                } else {
                    suggestions = emptyList()
                    isSearching = false
                }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.LocationOn, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; suggestions = emptyList(); onValueChange("", null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                } else {
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Default.Map, contentDescription = "Map", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        if (suggestions.isNotEmpty() || (query.isEmpty() && savedAddresses.isNotEmpty())) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    // 1. Map Fallback (Pinned)
                    item {
                        ListItem(
                            headlineContent = { Text("Choose on map", fontWeight = FontWeight.Bold, color = Color.Black) },
                            supportingContent = { Text("Pin precise location manually", color = Color.Gray) },
                            leadingContent = { Icon(Icons.Default.AddLocationAlt, null, tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.clickable { onOpenMap() },
                            colors = ListItemDefaults.colors(containerColor = Color.White)
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }

                    // 2. Saved Addresses (Only if query is empty)
                    if (query.isEmpty()) {
                        items(savedAddresses.take(3)) { addr ->
                            ListItem(
                                headlineContent = { Text(addr.label ?: "Saved Place", fontWeight = FontWeight.Bold, color = Color.Black) },
                                supportingContent = { Text(addr.address_text ?: "", maxLines = 1, color = Color.Gray) },
                                leadingContent = { 
                                    Icon(
                                        imageVector = if(addr.label == "Home") Icons.Default.Home else if(addr.label == "Work") Icons.Default.Work else Icons.Default.Star, 
                                        null, 
                                        tint = MaterialTheme.colorScheme.primary
                                    ) 
                                },
                                modifier = Modifier.clickable {
                                    query = addr.address_text ?: ""
                                    onValueChange(query, LatLng(addr.lat ?: 0.0, addr.lng ?: 0.0))
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.White)
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }

                    // 3. Live Suggestions
                    items(suggestions) { p ->
                        ListItem(
                            headlineContent = { Text(p.main_text, color = Color.Black, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(p.secondary_text, color = Color.DarkGray, maxLines = 1) },
                            leadingContent = { Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.outline) },
                            modifier = Modifier.clickable {
                                isSearching = true
                                query = p.description
                                scope.launch {
                                    try {
                                        val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                        onValueChange(details.formatted_address, LatLng(details.lat, details.lng))
                                        sessionToken = UUID.randomUUID().toString()
                                        suggestions = emptyList()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Failed to resolve address", Toast.LENGTH_SHORT).show()
                                    }
                                    isSearching = false
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.White)
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
