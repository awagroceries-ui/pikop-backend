package com.ng.pikop.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ng.pikop.R
import com.ng.pikop.core.datastore.TokenManager
import com.ng.pikop.core.network.ApiService
import com.ng.pikop.core.network.CreateCorporateRequest
import com.ng.pikop.core.network.ErrorUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorporateBusinessSetupScreen(
    onSetupSuccess: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var companyName by remember { mutableStateOf("") }
    var cacNumber by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var billingEmail by remember { mutableStateOf("") }
    
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
                text = "Business Verification",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Step 2: Tell us about your company.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Registered Company Name") },
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
                value = businessAddress,
                onValueChange = { businessAddress = it },
                label = { Text("Headquarters Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = billingEmail,
                onValueChange = { billingEmail = it },
                label = { Text("Billing / Finance Email") },
                modifier = Modifier.fillMaxWidth()
            )

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
                            val request = CreateCorporateRequest(
                                company_name = companyName,
                                billing_email = billingEmail,
                                cac_number = cacNumber,
                                business_address = businessAddress
                            )
                            val response = apiService.setupCorporateProfile(request)
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
                enabled = !isLoading && companyName.isNotBlank() && cacNumber.isNotBlank() && billingEmail.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Submit for Verification")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
