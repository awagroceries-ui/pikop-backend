package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.ErrorUtils
import com.ng.pikop.core.network.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit, 
    onUnverified: (String, String) -> Unit,
    onGoToSignup: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikop_logo),
                contentDescription = "Pikop Logo",
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val response = apiService.login(LoginRequest(email, password))
                            if (response.accessToken != null && response.refreshToken != null) {
                                val userRole = response.role ?: "CUSTOMER"
                                tokenManager.saveTokens(
                                    accessToken = response.accessToken,
                                    refreshToken = response.refreshToken,
                                    email = email,
                                    role = userRole,
                                    name = response.full_name,
                                    phone = response.phone,
                                    isVerified = true,
                                    referralCode = response.referral_code
                                )
                                onLoginSuccess(userRole)
                            } else {
                                errorMessage = response.message
                            }
                        } catch (e: Exception) {
                            val error = ErrorUtils.parseError(e)
                            if (error.contains("ACCOUNT_UNVERIFIED")) {
                                // Extract email/role if possible or use current state
                                // Backend returns them in 403 body
                                onUnverified(email, "CUSTOMER") // Role fallback
                            } else {
                                errorMessage = error
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onGoToSignup) {
                Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
