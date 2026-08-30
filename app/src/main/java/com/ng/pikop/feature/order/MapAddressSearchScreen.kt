package com.ng.pikop.feature.order

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
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
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.5244, 3.3792), 15f)
    }

    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }
    var sessionToken by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    
    var currentResolvedAddress by remember { mutableStateOf("Locating...") }
    var isGeocoding by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                location?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 17f)
                }
            } catch (_: Exception) {}
        }
        delay(500)
        focusRequester.requestFocus()
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            isGeocoding = true
            val address = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val results = geocoder.getFromLocation(center.latitude, center.longitude, 1)
                    results?.getOrNull(0)?.getAddressLine(0) ?: "Custom Location"
                } catch (_: Exception) { "Error resolving address" }
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
                            if (it.length >= 3) {
                                searchJob = scope.launch {
                                    delay(300)
                                    isSearchingSuggestions = true
                                    try {
                                        val res = apiService.getAutocomplete(it, sessionToken)
                                        suggestions = res.predictions
                                    } catch (_: Exception) { suggestions = emptyList() }
                                    finally { isSearchingSuggestions = false }
                                }
                            } else {
                                suggestions = emptyList()
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
                                IconButton(onClick = { query = ""; suggestions = emptyList() }) {
                                    Icon(Icons.Default.Clear, null, tint = Color.Gray)
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = query.isNotEmpty() || suggestions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        if (isSearchingSuggestions && suggestions.isEmpty()) {
                            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
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

        FloatingActionButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    scope.launch {
                        val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                        loc?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 17f)) }
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 160.dp),
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, "Locate Me")
        }
    }
}
