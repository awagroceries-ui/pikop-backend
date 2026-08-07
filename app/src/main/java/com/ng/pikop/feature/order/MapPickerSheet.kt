package com.ng.pikop.feature.order

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    // Default to Port Harcourt center
    val defaultLocation = LatLng(4.8156, 7.0498)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    
    var currentAddress by remember { mutableStateOf("Locating...") }
    var selectedLatLng by remember { mutableStateOf(defaultLocation) }
    var isGeocoding by remember { mutableStateOf(false) }

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
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
            )
            
            // Fixed Pin in the center
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .padding(bottom = 24.dp) // Offset to sit on top of the target point
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
