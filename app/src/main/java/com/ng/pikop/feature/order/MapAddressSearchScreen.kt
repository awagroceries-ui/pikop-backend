package com.ng.pikop.feature.order

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.AutocompletePrediction
import com.ng.pikop.core.network.SavedAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAddressSearchScreen(
    title: String,
    onBack: () -> Unit,
    onAddressSelected: (String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var hasResolvedInitialLocation by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        // Default to Lagos, Nigeria
        position = CameraPosition.fromLatLngZoom(LatLng(6.5244, 3.3792), 12f)
    }

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    var currentResolvedAddress by remember { mutableStateOf("Locating...") }
    var isGeocoding by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }

    fun resolveAndCenterOnUserLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        scope.launch {
            try {
                // 1. Instant Jump: Try last known location first
                val lastLoc = fusedLocationClient.lastLocation.await()
                if (lastLoc != null) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lastLoc.latitude, lastLoc.longitude), 17f)
                    hasResolvedInitialLocation = true
                }

                // 2. Fresh Fix: Request high accuracy update
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                    .setMaxUpdates(1)
                    .build()
                
                fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { loc ->
                            scope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 17f))
                                hasResolvedInitialLocation = true
                            }
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }, Looper.getMainLooper())
                
            } catch (e: Exception) {
                android.util.Log.e("PikopMap", "Location resolution failed: ${e.message}")
                hasResolvedInitialLocation = true 
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) resolveAndCenterOnUserLocation()
        else hasResolvedInitialLocation = true 
    }

    LaunchedEffect(Unit) {
        // Load shortcuts immediately
        try {
            val response = apiService.getSavedAddresses()
            savedAddresses = response.addresses
        } catch (_: Exception) {}

        // Handle GPS flow
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            resolveAndCenterOnUserLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        delay(600)
        focusRequester.requestFocus()
    }

    // Draggable Pin Geocoding with reliable triggering
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && hasResolvedInitialLocation) {
            val center = cameraPositionState.position.target
            isGeocoding = true
            val address = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                    results?.firstOrNull()?.getAddressLine(0) ?: "Custom Location"
                } catch (e: Exception) { 
                    android.util.Log.e("MapSearch", "Geocode error", e)
                    "Custom Pin Location" 
                }
            }
            currentResolvedAddress = address
            isGeocoding = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            )
        )

        // Bolt-style fixed center pin
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Center)
                .padding(bottom = 32.dp)
                .zIndex(2f)
        )

        // Unified Search & Suggestion Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 32.dp)
                .zIndex(10f)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            searchJob?.cancel()
                            if (it.length >= 2) {
                                searchJob = scope.launch {
                                    delay(200) // Lower delay for faster feedback
                                    isSearchingSuggestions = true
                                    searchError = null
                                    try {
                                        val center = cameraPositionState.position.target
                                        val res = apiService.getAutocomplete(it, sessionToken, center.latitude, center.longitude)
                                        if (res.success) {
                                            suggestions = res.predictions
                                        } else {
                                            searchError = res.error ?: "Service unavailable"
                                            suggestions = emptyList()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("AddressSearch", "Autocomplete failed", e)
                                        searchError = "Check your connection"
                                        suggestions = emptyList()
                                    } finally {
                                        isSearchingSuggestions = false
                                    }
                                }
                            } else {
                                suggestions = emptyList()
                                isSearchingSuggestions = false
                            }
                        },
                        placeholder = { Text("Search for $title...", color = Color.Gray) },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; suggestions = emptyList(); isSearchingSuggestions = false }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.Gray)
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = query.isEmpty() || suggestions.isNotEmpty() || isSearchingSuggestions || searchError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        if (isSearchingSuggestions) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        
                        if (searchError != null && query.isNotEmpty()) {
                            Text(
                                text = searchError!!, 
                                color = Color.Red, 
                                style = MaterialTheme.typography.bodySmall, 
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            if (query.isEmpty()) {
                                // Shortcuts
                                item {
                                    ListItem(
                                        headlineContent = { Text("Use my current location", fontWeight = FontWeight.Bold) },
                                        leadingContent = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier.clickable { resolveAndCenterOnUserLocation() },
                                        colors = ListItemDefaults.colors(containerColor = Color.White)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                }
                                items(savedAddresses) { addr ->
                                    ListItem(
                                        headlineContent = { Text(addr.label ?: "Saved Place", fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(addr.address_text ?: "", maxLines = 1) },
                                        leadingContent = { Icon(if (addr.label == "Home") Icons.Default.Home else Icons.Default.Work, null, tint = MaterialTheme.colorScheme.primary) },
                                        modifier = Modifier.clickable {
                                            scope.launch {
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(addr.lat ?: 0.0, addr.lng ?: 0.0), 17f))
                                                focusManager.clearFocus()
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.White)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }

                            items(suggestions) { p ->
                                ListItem(
                                    headlineContent = { Text(p.main_text, fontWeight = FontWeight.Bold, color = Color.Black) },
                                    supportingContent = { Text(p.secondary_text, color = Color.DarkGray, maxLines = 1) },
                                    leadingContent = { Icon(Icons.Default.Place, null, tint = Color.Gray) },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            focusManager.clearFocus()
                                            query = ""
                                            suggestions = emptyList()
                                            try {
                                                val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(details.lat, details.lng), 17f))
                                                sessionToken = UUID.randomUUID().toString()
                                            } catch (_: Exception) {}
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
        }

        // Fixed bottom confirmation sheet
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .zIndex(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (title.lowercase().contains("pickup")) "Confirm Pickup" else "Confirm Delivery",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentResolvedAddress,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    maxLines = 2,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAddressSelected(currentResolvedAddress, cameraPositionState.position.target.latitude, cameraPositionState.position.target.longitude) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isGeocoding,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isGeocoding) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Confirm Location", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Persistent Locate FAB
        FloatingActionButton(
            onClick = { resolveAndCenterOnUserLocation() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 160.dp),
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, "Locate Me")
        }
    }
}
