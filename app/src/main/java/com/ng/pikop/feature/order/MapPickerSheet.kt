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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.AutocompletePrediction
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
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(4.8156, 7.0498), 15f)
    }
    
    var currentAddress by remember { mutableStateOf("Locating...") }
    var selectedLatLng by remember { mutableStateOf(LatLng(4.8156, 7.0498)) }
    var isGeocoding by remember { mutableStateOf(false) }

    // Advanced Search State
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    fun centerOnUser() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scope.launch {
                try {
                    val location: Location? = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        userLocation = latLng
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "GPS Signal weak", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) { centerOnUser() }

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
                        results?.getOrNull(0)?.getAddressLine(0) ?: "Unknown Location"
                    } catch (e: Exception) { "Error fetching address" }
                }
                currentAddress = address
                isGeocoding = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        rotationGesturesEnabled = false
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    )
                )

                // Search UI Layer (Higher Z-Index)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 32.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(10f)
                ) {
                    Column {
                        // Search Bar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                                IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black) }
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        searchJob?.cancel()
                                        if (it.length >= 2) {
                                            searchJob = scope.launch {
                                                kotlinx.coroutines.delay(400)
                                                isSearchingSuggestions = true
                                                try {
                                                    val res = apiService.getAutocomplete(it, sessionToken, userLocation?.latitude, userLocation?.longitude)
                                                    searchSuggestions = res.predictions
                                                } catch (_: Exception) { searchSuggestions = emptyList() }
                                                finally { isSearchingSuggestions = false }
                                            }
                                        } else { searchSuggestions = emptyList() }
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Search location...", color = Color.Gray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    ),
                                    singleLine = true
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; searchSuggestions = emptyList() }) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Suggestions List (Integrated Overlay)
                        if (searchSuggestions.isNotEmpty() || isSearchingSuggestions) {
                            Card(
                                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSearchingSuggestions && searchSuggestions.isEmpty()) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                                }
                                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                                    items(searchSuggestions) { p ->
                                        SuggestionRow(
                                            prediction = p,
                                            onSelect = {
                                                scope.launch {
                                                    try {
                                                        isGeocoding = true
                                                        val details = apiService.getPlaceDetails(p.place_id, sessionToken)
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(details.lat, details.lng), 17f))
                                                        searchQuery = ""
                                                        searchSuggestions = emptyList()
                                                        sessionToken = UUID.randomUUID().toString()
                                                    } catch (_: Exception) {} finally { isGeocoding = false }
                                                }
                                            },
                                            onRefine = { searchQuery = p.main_text }
                                        )
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                                    }
                                    item {
                                        ListItem(
                                            headlineContent = { Text("Choose on map", fontWeight = FontWeight.Bold) },
                                            leadingContent = { Icon(Icons.Default.AddLocationAlt, null, tint = MaterialTheme.colorScheme.secondary) },
                                            modifier = Modifier.clickable { searchSuggestions = emptyList(); searchQuery = "" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Fixed Center Pin
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp).align(Alignment.Center).padding(bottom = 24.dp).zIndex(1f)
                )

                // Locate Me FAB
                FloatingActionButton(
                    onClick = { centerOnUser() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 160.dp).zIndex(1f),
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.MyLocation, "Locate Me") }

                // Bottom Panel
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).zIndex(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Confirm Point", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(text = currentAddress, style = MaterialTheme.typography.bodyLarge, color = Color.Black, maxLines = 2, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onLocationSelected(currentAddress, selectedLatLng) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isGeocoding,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            if (isGeocoding) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            else Text("Confirm Location", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionRow(prediction: AutocompletePrediction, onSelect: () -> Unit, onRefine: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = prediction.main_text, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
            Text(text = prediction.secondary_text, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        }
        IconButton(onClick = onRefine) {
            Icon(Icons.Default.NorthWest, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        }
    }
}
