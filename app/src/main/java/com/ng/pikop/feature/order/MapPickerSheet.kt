package com.ng.pikop.feature.order

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
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
    
    // City Constants
    val portHarcourt = LatLng(4.8156, 7.0498)
    val lagos = LatLng(6.5244, 3.3792)
    val abuja = LatLng(9.0578, 7.4951)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(portHarcourt, 15f)
    }
    
    var currentAddress by remember { mutableStateOf("Locating...") }
    var selectedLatLng by remember { mutableStateOf(portHarcourt) }
    var isGeocoding by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Request Location Permission & Move Camera to User
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isChecked(isGranted)) {
            scope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    }
                } catch (e: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(userLatLng, 15f)
                }
            } catch (e: Exception) {}
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            )
            
            // City Selector UI
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                CityChip("Port Harcourt", onClick = {
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(portHarcourt, 15f)) }
                })
                CityChip("Lagos", onClick = {
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(lagos, 15f)) }
                })
                CityChip("Abuja", onClick = {
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(abuja, 15f)) }
                })
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Fixed Pin in the center
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .padding(bottom = 24.dp)
            )

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
