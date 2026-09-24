package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.SignupRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupFulfillerScreen(
    category: String,
    onSignupSuccess: (String, String) -> Unit,
    onViewTerms: () -> Unit,
    onViewPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var fleetInviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var cityRequiresPermit by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Extra fields with native pickers
    var dateOfBirth by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var homeAddress by remember { mutableStateOf("") }

    // Nigeria State & City Location Mapping
    val nigeriaLocations = remember {
        mapOf(
            "Lagos" to listOf("Ikeja", "Lekki", "Victoria Island", "Surulere", "Ikorodu", "Ajah", "Badagry"),
            "Rivers" to listOf("Port Harcourt", "Obio-Akpor", "Eleme", "Bonny", "Onne"),
            "FCT (Abuja)" to listOf("Abuja Municipal", "Gwarinpa", "Wuse", "Asokoro", "Maitama", "Kubwa", "Gwagwalada"),
            "Oyo" to listOf("Ibadan", "Ogbomosho", "Oyo Town"),
            "Kano" to listOf("Kano City"),
            "Delta" to listOf("Asaba", "Warri", "Sapele"),
            "Edo" to listOf("Benin City"),
            "Anambra" to listOf("Awka", "Onitsha", "Nnewi"),
            "Enugu" to listOf("Enugu City", "Nsukka"),
            "Kaduna" to listOf("Kaduna City", "Zaria"),
            "Ogun" to listOf("Abeokuta", "Ijebu-Ode", "Sango Ota"),
            "Akwa Ibom" to listOf("Uyo", "Eket"),
            "Abia" to listOf("Aba", "Umuahia"),
            "Cross River" to listOf("Calabar"),
            "Imo" to listOf("Owerri"),
            "Plateau" to listOf("Jos")
        )
    }

    var operatingState by remember { mutableStateOf("") }
    var expandedStateDropdown by remember { mutableStateOf(false) }

    var operatingCity by remember { mutableStateOf("") }
    var expandedCityDropdown by remember { mutableStateOf(false) }

    var gender by remember { mutableStateOf("") }
    var expandedGenderDropdown by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female", "Other")
    
    var registrationNumber by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    
    val isRiderOrDriver = category == "RIDER" || category == "DRIVER"
    val isRider = category == "RIDER"
    val isDriver = category == "DRIVER"
    
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(Unit) {
        tokenManager.clearTokens()
    }

    LaunchedEffect(operatingCity) {
        if (operatingCity.length >= 3) {
            try {
                val rules = apiService.getCityRules(operatingCity)
                cityRequiresPermit = rules["requires_rider_permit"] as? Boolean ?: false
            } catch (_: Exception) {}
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        dateOfBirth = formatter.format(Date(millis))
                    }
                    showDatePickerDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join the Fleet",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Text(
                text = "Start earning by delivering items.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number (+234...)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                isError = confirmPassword.isNotEmpty() && password != confirmPassword
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Native Material 3 Date Picker Field
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth") },
                    placeholder = { Text("Select Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerDialog = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePickerDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                label = { Text("Home Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Operating State Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedStateDropdown,
                onExpandedChange = { expandedStateDropdown = it }
            ) {
                OutlinedTextField(
                    value = operatingState,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Operating State") },
                    placeholder = { Text("Select State") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStateDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedStateDropdown,
                    onDismissRequest = { expandedStateDropdown = false }
                ) {
                    nigeriaLocations.keys.forEach { state ->
                        DropdownMenuItem(
                            text = { Text(state) },
                            onClick = {
                                if (operatingState != state) {
                                    operatingState = state
                                    operatingCity = "" // Reset city when state changes
                                }
                                expandedStateDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Operating City Dropdown
            val availableCities = nigeriaLocations[operatingState] ?: emptyList()

            ExposedDropdownMenuBox(
                expanded = expandedCityDropdown && operatingState.isNotBlank(),
                onExpandedChange = { if (operatingState.isNotBlank()) expandedCityDropdown = it }
            ) {
                OutlinedTextField(
                    value = operatingCity,
                    onValueChange = {},
                    readOnly = true,
                    enabled = operatingState.isNotBlank(),
                    label = { Text("Operating City") },
                    placeholder = { Text(if (operatingState.isBlank()) "Select State First" else "Select City") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCityDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                if (availableCities.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedCityDropdown,
                        onDismissRequest = { expandedCityDropdown = false }
                    ) {
                        availableCities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    operatingCity = city
                                    expandedCityDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Gender Dropdown Selection
            ExposedDropdownMenuBox(
                expanded = expandedGenderDropdown,
                onExpandedChange = { expandedGenderDropdown = it }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGenderDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedGenderDropdown,
                    onDismissRequest = { expandedGenderDropdown = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                expandedGenderDropdown = false
                            }
                        )
                    }
                }
            }

            if (isRiderOrDriver) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Vehicle Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it },
                    label = { Text("Plate Number / Registration") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text("Brand / Make (e.g. Honda, Toyota)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Required Documents (Upload via Dashboard after signup)", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                Text("• Valid Driver's / Rider's License", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                if (isRider && cityRequiresPermit) {
                    Text("• Commercial Rider Permit (This permit is mandatory for riders in $operatingCity — you must provide this to be approved for missions)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                
                if (isDriver) {
                    Text("• Vehicle Insurance & Roadworthiness", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = referralCode,
                onValueChange = { referralCode = it },
                label = { Text("Referral Code (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = fleetInviteCode,
                onValueChange = { fleetInviteCode = it },
                label = { Text("Fleet Partner Invite Code (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val annotatedString = buildAnnotatedString {
                append("By clicking Sign Up, you agree to Pikop's ")
                
                pushStringAnnotation(tag = "terms", annotation = "terms")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Terms & Conditions")
                }
                pop()
                
                append(" and ")
                
                pushStringAnnotation(tag = "privacy", annotation = "privacy")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Privacy Policy")
                }
                pop()
                
                append(".")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset)
                        .firstOrNull()?.let { onViewTerms() }
                    
                    annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                        .firstOrNull()?.let { onViewPrivacy() }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val request = SignupRequest(
                                full_name = fullName,
                                email = email,
                                phone = phone,
                                password = password,
                                role = "FULFILLER",
                                referral_code = referralCode.ifBlank { null },
                                primary_class = category,
                                date_of_birth = dateOfBirth,
                                home_address = if (operatingState.isNotBlank()) "$homeAddress, $operatingCity, $operatingState State" else "$homeAddress, $operatingCity",
                                gender = gender,
                                current_state = operatingState.ifBlank { null },
                                registration_number = if (isRiderOrDriver) registrationNumber else null,
                                make = if (isRiderOrDriver) make else null,
                                model = if (isRiderOrDriver) model else null,
                                color = if (isRiderOrDriver) color else null,
                                terms_version = "0.1",
                                privacy_version = "0.1",
                                fleet_invite_code = fleetInviteCode.ifBlank { null }
                            )
                            val response = apiService.signup(request)
                            if (response.message?.contains("registered", ignoreCase = true) == true) {
                                onSignupSuccess(email, "FULFILLER")
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
                enabled = !isLoading && 
                          fullName.isNotBlank() && 
                          email.isNotBlank() && 
                          phone.isNotBlank() && 
                          password.isNotBlank() && 
                          password == confirmPassword &&
                          operatingState.isNotBlank() &&
                          operatingCity.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up as Fulfiller")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
