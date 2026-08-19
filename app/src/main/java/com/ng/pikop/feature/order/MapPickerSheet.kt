package com.ng.pikop.feature.order

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerSheet(
    onDismiss: () -> Unit,
    onLocationSelected: (String, LatLng) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.8156, 7.0498), 15f) // Default PH
    }
    
    var currentAddress by remember { mutableStateOf("Locating...") }
    var selectedLatLng by remember { mutableStateOf(LatLng(4.8156, 7.0498)) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Search Bar State
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<com.ng.pikop.core.network.AutocompletePrediction>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    var sessionToken by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Auto-Locate on Launch
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location = fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).await()
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f))
                }
            } catch (e: Exception) {}
        }
    }

    // Reverse Geocoding Logic
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            selectedLatLng = center
            isGeocoding = true
            
            scope.launch {
                val address = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val results = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                        if (results?.isNotEmpty() == true) {
                            results[0].getAddressLine(0)
                        } else "Unknown Location"
                    } catch (e: Exception) {
                        "Error fetching address"
                    }
                }
                currentAddress = address
                isGeocoding = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false, 
                    myLocationButtonEnabled = false,
                    scrollGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    rotationGesturesEnabled = true
                ),
                properties = MapProperties(isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            )
            
            // Search Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.length > 2) {
                            scope.launch {
                                try {
                                    val res = apiService.getAutocomplete(it, sessionToken)
                                    searchSuggestions = res.predictions
                                } catch (_: Exception) {}
                            }
                        } else {
                            searchSuggestions = emptyList()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search location...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = ""; searchSuggestions = emptyList() }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                if (searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier.padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(searchSuggestions) { p ->
                                ListItem(
                                    headlineContent = { Text(p.main_text, color = Color.Black, fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text(p.secondary_text, color = Color.Gray, maxLines = 1) },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            try {
                                                val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(details.lat, details.lng), 17f))
                                                searchQuery = ""
                                                searchSuggestions = emptyList()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                )
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Fixed Pin in the center
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .padding(bottom = 24.dp)
            )

            // Locate Me Button
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        try {
                            val location = fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).await()
                            if (location != null) {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f))
                            }
                        } catch (e: SecurityException) {}
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 120.dp),
                containerColor = Color.White,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, "Locate Me")
            }

            // Bottom Confirm Panel
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Drop Pin at", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = currentAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onLocationSelected(currentAddress, selectedLatLng) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeocoding
                    ) {
                        if (isGeocoding) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Confirm Location")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CityChip(label: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}

fun isChecked(granted: Boolean) = granted
