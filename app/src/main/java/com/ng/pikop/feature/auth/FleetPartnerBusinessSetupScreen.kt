package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.SetupFleetRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetPartnerBusinessSetupScreen(
    onSetupSuccess: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var businessName by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var cacNumber by remember { mutableStateOf("") }
    var fleetSize by remember { mutableStateOf("") }
    
    var city1 by remember { mutableStateOf("") }
    var vehicleBike by remember { mutableStateOf(false) }
    var vehicleCar by remember { mutableStateOf(false) }
    var vehicleVan by remember { mutableStateOf(false) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fleet Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Step 2: Tell us about your logistics company.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Registered Business Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = businessAddress,
                onValueChange = { businessAddress = it },
                label = { Text("Business Address / HQ") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = cacNumber,
                onValueChange = { cacNumber = it },
                label = { Text("CAC Registration Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fleetSize,
                onValueChange = { fleetSize = it },
                label = { Text("Approximate Fleet Size (Total drivers)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Operations Details", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = city1,
                onValueChange = { city1 = it },
                label = { Text("Primary City of Operation") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Vehicle Types Operated:", style = MaterialTheme.typography.labelSmall)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vehicleBike, onCheckedChange = { vehicleBike = it })
                Text("Motorcycles / Bikes")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vehicleCar, onCheckedChange = { vehicleCar = it })
                Text("Cars / Sedans")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = vehicleVan, onCheckedChange = { vehicleVan = it })
                Text("Vans / Trucks")
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val types = mutableListOf<String>()
                            if (vehicleBike) types.add("bike")
                            if (vehicleCar) types.add("car")
                            if (vehicleVan) types.add("van")

                            val request = SetupFleetRequest(
                                business_name = businessName,
                                cac_number = cacNumber,
                                address = businessAddress,
                                fleet_size = fleetSize.toIntOrNull() ?: 0,
                                vehicle_types = types,
                                cities = listOf(city1)
                            )
                            val response = apiService.setupFleetProfile(request)
                            if (response.success == true) {
                                onSetupSuccess()
                            } else {
                                errorMessage = response.message
                            }
                        } catch (e: Exception) {
                            errorMessage = ErrorUtils.parseError(e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && businessName.isNotBlank() && cacNumber.isNotBlank() && businessAddress.isNotBlank() && city1.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit for Review")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
