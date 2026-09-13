package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    var isAccepted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Extra fields
    var dateOfBirth by remember { mutableStateOf("") }
    var homeAddress by remember { mutableStateOf("") }
    var operatingCity by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    
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

            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { dateOfBirth = it },
                label = { Text("Date of Birth (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = homeAddress,
                onValueChange = { homeAddress = it },
                label = { Text("Home Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = operatingCity,
                onValueChange = { operatingCity = it },
                label = { Text("Operating City (e.g. Port Harcourt)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Gender (Male/Female/Other)") },
                modifier = Modifier.fillMaxWidth()
            )

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
                
                if (isRider) {
                    val showPortHarcourtTip = operatingCity.contains("Port Harcourt", ignoreCase = true) || operatingCity.contains("PH", ignoreCase = true)
                    if (showPortHarcourtTip) {
                        Text("• Commercial Rider Permit (This permit is required for riders operating in Port Harcourt — you may be asked to provide this before being approved to accept missions there)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("• Commercial Rider Permit (Optional, depending on city regulations)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAccepted,
                    onCheckedChange = { isAccepted = it }
                )
                Text(
                    text = "I accept the Terms & Conditions and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                home_address = "$homeAddress, $operatingCity",
                                gender = gender,
                                registration_number = if (isRiderOrDriver) registrationNumber else null,
                                make = if (isRiderOrDriver) make else null,
                                model = if (isRiderOrDriver) model else null,
                                color = if (isRiderOrDriver) color else null
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
                          isAccepted
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

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedString = buildAnnotatedString {
                append("By signing up, you agree to our ")
                
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
