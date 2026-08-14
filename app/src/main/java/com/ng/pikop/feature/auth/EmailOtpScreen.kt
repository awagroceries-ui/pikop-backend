package com.ng.pikop.feature.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.VerifyEmailRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailOtpScreen(
    email: String, 
    onVerificationSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableStateOf(0) }
    var isRateLimited by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create(tokenManager) }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown -= 1
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter the 6-digit code sent to $email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = { Text("OTP Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = apiService.verifyEmail(VerifyEmailRequest(email, otp))
                            if (response.accessToken != null) {
                                tokenManager.saveTokens(
                                    accessToken = response.accessToken,
                                    refreshToken = response.refreshToken ?: "",
                                    email = email,
                                    role = response.role ?: "CUSTOMER",
                                    name = response.full_name,
                                    phone = response.phone,
                                    isVerified = true,
                                    referralCode = response.referral_code
                                )
                                onVerificationSuccess()
                            } else {
                                errorMessage = response.message ?: "Invalid verification code"
                            }
                        } catch (e: Exception) {
                            errorMessage = ErrorUtils.parseError(e)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && otp.length == 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Verify")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    if (resendCooldown > 0 || isLoading || isRateLimited) return@TextButton
                    coroutineScope.launch {
                        isLoading = true
                        try {
                            val response = apiService.resendOtp(mapOf("email" to email))
                            Toast.makeText(context, response.message ?: "New code sent!", Toast.LENGTH_SHORT).show()
                            resendCooldown = 30
                        } catch (e: Exception) {
                            val error = ErrorUtils.parseError(e)
                            if (error.contains("RATE_LIMITED")) {
                                isRateLimited = true
                            }
                            errorMessage = error
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = resendCooldown == 0 && !isRateLimited
            ) {
                Text(
                    text = if (isRateLimited) "Too many attempts — try again later"
                          else if (resendCooldown > 0) "Resend code in ${resendCooldown}s" 
                          else "Didn't receive a code? Resend",
                    color = if (resendCooldown > 0 || isRateLimited) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onLogout) {
                Text("Sign out and use a different account", color = Color.Gray)
            }
        }
    }
}
