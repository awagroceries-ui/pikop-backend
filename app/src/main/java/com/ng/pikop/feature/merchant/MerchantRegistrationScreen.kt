package com.ng.pikop.feature.merchant

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantRegistrationScreen(
    navController: NavController,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Pickup Location State (From Map Search)
    val pickupAddress by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<String>("merchant_address")
        ?.observeAsState("") ?: remember { mutableStateOf("") }
    
    val pickupLat by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Double>("merchant_lat")
        ?.observeAsState(0.0) ?: remember { mutableStateOf(0.0) }
        
    val pickupLng by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Double>("merchant_lng")
        ?.observeAsState(0.0) ?: remember { mutableStateOf(0.0) }

    val tokenManager = remember { TokenManager(context) }
    val apiService = remember { ApiService.create(tokenManager) }
    val scope = rememberCoroutineScope()

    var businessName by remember { mutableStateOf("") }
    var cacNumber by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Port Harcourt") }
    var description by remember { mutableStateOf("") }
    
    var merchantType by remember { mutableStateOf("MARKETPLACE") } // MARKETPLACE or KITCHEN
    var cuisineType by remember { mutableStateOf("") }
    
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merchant Onboarding", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Business Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            // Merchant Type Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = merchantType == "MARKETPLACE",
                    onClick = { merchantType = "MARKETPLACE" },
                    label = { Text("General Vendor") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = merchantType == "KITCHEN",
                    onClick = { merchantType = "KITCHEN" },
                    label = { Text("Cloud Kitchen") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cacNumber,
                onValueChange = { cacNumber = it },
                label = { Text("CAC Number (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = contactEmail,
                onValueChange = { contactEmail = it },
                label = { Text("Contact Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            if (merchantType == "KITCHEN") {
                OutlinedTextField(
                    value = cuisineType,
                    onValueChange = { cuisineType = it },
                    label = { Text("Cuisine Type (e.g. African, Fast Food)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Business Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Business Location Picker
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Business Location", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            
            OutlinedCard(
                onClick = { navController.navigate("map_address_search/Business Location/merchant") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (pickupAddress.isBlank()) "Select Location" else pickupAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Tap to set your permanent pickup point", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Payout Details (Bank Account)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if (it.length <= 10) accountNumber = it },
                label = { Text("Account Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        try {
                            if (merchantType == "MARKETPLACE") {
                                apiService.registerVendor(VendorRegistrationRequest(
                                    business_name = businessName,
                                    cac_number = cacNumber.ifBlank { null },
                                    contact_email = contactEmail,
                                    city = city,
                                    description = description,
                                    bank_account_name = accountName,
                                    bank_account_number = accountNumber,
                                    bank_code = "" // Simplified
                                ))
                            } else {
                                apiService.registerKitchen(KitchenRegistrationRequest(
                                    business_name = businessName,
                                    cac_number = cacNumber.ifBlank { null },
                                    contact_email = contactEmail,
                                    city = city,
                                    cuisine_type = cuisineType,
                                    description = description,
                                    bank_account_name = accountName,
                                    bank_account_number = accountNumber,
                                    bank_code = ""
                                ))
                            }
                            Toast.makeText(context, "Application Submitted Successfully!", Toast.LENGTH_LONG).show()
                            onSuccess()
                        } catch (e: Exception) {
                            val errorMsg = ErrorUtils.parseError(e)
                            Toast.makeText(context, "Registration Failed: $errorMsg", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && businessName.isNotBlank() && contactEmail.isNotBlank() && accountNumber.length == 10 && pickupAddress.isNotBlank()
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("Submit Application")
            }
            
            Text(
                "Your application will be reviewed by our team. This process typically takes 48 hours.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
