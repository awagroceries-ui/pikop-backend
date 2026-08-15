package com.ng.pikop.feature.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isSearching by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(value) {
        if (value != query) {
            query = value
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                onValueChange(it, null)
                
                searchJob?.cancel()
                if (it.length > 2) {
                    searchJob = scope.launch {
                        delay(500) // Debounce
                        isSearching = true
                        val request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(it)
                            .setCountries("NG") // Restrict to Nigeria
                            .build()
                        
                        placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener { response ->
                                suggestions = response.autocompletePredictions
                                isSearching = false
                            }
                            .addOnFailureListener {
                                suggestions = emptyList()
                                isSearching = false
                            }
                    }
                } else {
                    suggestions = emptyList()
                    isSearching = false
                }
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { 
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                IconButton(onClick = onOpenMap) {
                    Icon(Icons.Default.Map, contentDescription = "Select on Map", tint = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(suggestions) { prediction ->
                        ListItem(
                            headlineContent = { Text(prediction.getPrimaryText(null).toString()) },
                            supportingContent = { Text(prediction.getSecondaryText(null).toString(), style = MaterialTheme.typography.bodySmall) },
                            leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) },
                            modifier = Modifier.clickable {
                                val address = prediction.getFullText(null).toString()
                                query = address
                                suggestions = emptyList()
                                
                                // Fetch coordinates for the selected place
                                val placeFields = listOf(Place.Field.LAT_LNG)
                                val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
                                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                                    onValueChange(address, response.place.latLng)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
