package com.ng.pikop.feature.order

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
        position = CameraPosition.fromLatLngZoom(LatLng(4.8156, 7.0498), 15f)
    }
    
    var currentAddress by remember { mutableStateOf("Locating...") }
    var selectedLatLng by remember { mutableStateOf(LatLng(4.8156, 7.0498)) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Search Bar State
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<com.ng.pikop.core.network.AutocompletePrediction>>(emptyList()) }
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun centerOnUser() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                try {
                    val location: Location? = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f))
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "GPS Signal weak", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-Locate on Launch
    LaunchedEffect(Unit) {
        centerOnUser()
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler { onDismiss() }
        
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Box(modifier = Modifier.fillMaxSize()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false, 
                        myLocationButtonEnabled = false,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        scrollGesturesEnabledDuringRotateOrZoom = true
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
                        minZoomPreference = 5f,
                        maxZoomPreference = 20f
                    )
                )
                
                // Search Overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                null, 
                                modifier = Modifier.padding(8.dp).size(24.dp),
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                searchJob?.cancel()
                                if (it.length > 2) {
                                    searchJob = scope.launch {
                                        isSearchingSuggestions = true
                                        kotlinx.coroutines.delay(300)
                                        try {
                                            android.util.Log.d("PikopMapSearch", "Autocomplete Fetching for: $it")
                                            val res = apiService.getAutocomplete(it, sessionToken)
                                            // Force UI update by clearing and re-setting
                                            searchSuggestions = emptyList()
                                            searchSuggestions = res.predictions
                                            android.util.Log.d("PikopMapSearch", "Autocomplete SUCCESS: ${res.predictions.size} items")
                                        } catch (e: Exception) {
                                            android.util.Log.e("PikopMapSearch", "Autocomplete FAIL: ${e.message}")
                                            searchSuggestions = emptyList()
                                        } finally {
                                            isSearchingSuggestions = false
                                        }
                                    }
                                } else {
                                    searchSuggestions = emptyList()
                                    isSearchingSuggestions = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search location...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; searchSuggestions = emptyList() }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                    }

                    if (searchSuggestions.isNotEmpty() || isSearchingSuggestions) {
                        Card(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            if (isSearchingSuggestions && searchSuggestions.isEmpty()) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                            }
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(searchSuggestions) { p ->
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                text = p.main_text, 
                                                color = Color.Black, 
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            ) 
                                        },
                                        supportingContent = { 
                                            Text(
                                                text = p.secondary_text, 
                                                color = Color.Gray, 
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodySmall
                                            ) 
                                        },
                                        leadingContent = { 
                                            Icon(
                                                imageVector = Icons.Default.Place, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            ) 
                                        },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                try {
                                                    isGeocoding = true
                                                    val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(details.lat, details.lng), 17f))
                                                    searchQuery = ""
                                                    searchSuggestions = emptyList()
                                                    sessionToken = java.util.UUID.randomUUID().toString()
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isGeocoding = false
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.White)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
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
                    onClick = { centerOnUser() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = 140.dp),
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Drop Pin at", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentAddress,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp),
                            maxLines = 2,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { onLocationSelected(currentAddress, selectedLatLng) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isGeocoding,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isGeocoding) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Confirm Location", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
