package com.ng.pikop.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Privacy Policy for Pikop",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A product of Awa Foods & Groceries",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = """
                        1. INTRODUCTION
                        At Pikop, we are committed to protecting your privacy and ensuring compliance with the Nigeria Data Protection Regulation (NDPR). This policy explains how we collect, use, and safeguard your personal information.

                        2. INFORMATION WE COLLECT
                        We collect information to provide better services, including:
                        - Account Info: Name, email, phone number, and password.
                        - Location Data: Precise GPS coordinates for real-time tracking of pickups and deliveries.
                        - Device Info: Device model, OS version, and unique identifiers.
                        - Transaction History: Details of your delivery requests and payment references (processed via Paystack).

                        3. HOW WE USE YOUR DATA
                        Your data is used to:
                        - Enable real-time dispatching and tracking.
                        - Facilitate secure payments.
                        - Verify Fulfiller identity (KYC documents).
                        - Provide customer support.

                        4. DATA SHARING
                        We only share data necessary for delivery:
                        - Users see Fulfiller's name, photo (if available), and live location.
                        - Fulfillers see User's name, pickup/delivery address, and phone number.
                        We do not sell your personal data to third parties.

                        5. DATA STORAGE AND SECURITY
                        Your data is stored securely on encrypted servers. While we take every precaution, no internet transmission is 100% secure. You are responsible for keeping your password confidential.

                        6. YOUR RIGHTS
                        Under the NDPR, you have the right to access, correct, or request the deletion of your personal data. You may contact us at privacy@awa.name.ng for such requests.

                        7. CHANGES TO THIS POLICY
                        We may update this policy periodically. Your continued use of the app after changes indicates acceptance of the revised terms.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
